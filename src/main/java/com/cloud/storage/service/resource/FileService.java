package com.cloud.storage.service.resource;

import com.cloud.storage.config.minio.MinioConfigProperties;
import com.cloud.storage.dto.resource.ResourceInfo;
import com.cloud.storage.dto.resource.ResourceType;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;
import java.util.zip.ZipException;

import static com.cloud.storage.util.PathUtils.*;

@Service
public class FileService {
    private final String bucketName;
    private final MinioService minioService;

    public FileService(MinioConfigProperties minioConfig, MinioService minioService) {
        this.bucketName = minioConfig.getBucketName();
        this.minioService = minioService;
    }

    public void createUserBucket(Integer userId) {
        String fullPath = getFullPathById("", userId);

        boolean found = minioService.isExist(bucketName);
        if (!found) {
            minioService.makeBucket(bucketName);
        }

        minioService.putObject(bucketName, fullPath);
    }

    public ResourceInfo createDirectory(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);
        String directoryPath = setDirectoryLink(path);
        String fullPath = getFullPathById(directoryPath, id);

        minioService.putObject(bucketName, fullPath);

        return getDirectoryInfo(directoryPath, id);
    }

    public List<ResourceInfo> getDirectoryContents(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);
        String directoryPath = path.replaceAll("/+$", "") + "/";
        String fullPath = getFullPathById(directoryPath, id);

        List<ResourceInfo> contents = new ArrayList<>();
        Iterable<Result<Item>> results = minioService.getDirectoryContent(bucketName, fullPath);

        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectPath = item.objectName().replace(String.format("user-%d-files/", id), "");

                if (objectPath.startsWith("/")) {
                    objectPath = objectPath.substring(1);
                }

                if (objectPath.isEmpty() || objectPath.equals(directoryPath) || objectPath.equals(fullPath)) {
                    continue;
                }

                if (item.isDir()) {
                    contents.add(getDirectoryInfo(objectPath, id));
                } else {
                    contents.add(getFileInfo(objectPath, id));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to get directory contents");
        }

        return contents;
    }

    public List<ResourceInfo> uploadResource(String encodedPath, List<MultipartFile> files, Integer id) {
        String path = decodeString(encodedPath);
        String normalizedBase = setDirectoryLink(path);

        List<ResourceInfo> uploadedResources = Collections.synchronizedList(new ArrayList<>());
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            String normalizedFilename = originalFilename.startsWith("/") ? originalFilename.substring(1) : originalFilename;
            String fullPath = getFullPathById(normalizedBase + normalizedFilename, id);

            CompletableFuture<Void> future = minioService.uploadResource(bucketName, fullPath, file)
                    .thenRun(() -> uploadedResources.add(createResourceInfo(normalizedBase + normalizedFilename, id)));
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return uploadedResources;
    }

    private ResourceInfo createResourceInfo(String path, Integer id) {
        String parentPath = getParentPath(path);
        String name = getFilenameFromPath(path);
        String fullPath = getFullPathById(path, id);

        if (isDirectory(path)) {
            return new ResourceInfo(parentPath, name, null, ResourceType.DIRECTORY);
        } else {
            StatObjectResponse stat = minioService.getStatistics(bucketName, fullPath);
            return new ResourceInfo(parentPath, name, stat.size(), ResourceType.FILE);
        }
    }

    public List<ResourceInfo> searchResource(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);

        Iterable<Result<Item>> results = minioService.searchResource(bucketName, path);

        List<ResourceInfo> foundResources = new ArrayList<>();
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String objectPath = item.objectName();

                if (item.isDir()) {
                    foundResources.add(getDirectoryInfo(objectPath, id));
                } else {
                    foundResources.add(getFileInfo(objectPath, id));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search resource");
        }

        return foundResources;
    }

    public ResourceInfo moveResource(String encodedFromPath, String encodedToPath, Integer id) {
        String fullFromPath = getFullPathById(decodeString(encodedFromPath), id);
        String fullToPath = getFullPathById(decodeString(encodedToPath), id);

        StatObjectResponse stat = minioService.getStatistics(bucketName, fullFromPath);
        minioService.copyObject(bucketName, fullFromPath, fullToPath);
        minioService.removeObject(bucketName, fullFromPath);

        String parentPath = getParentPath(fullToPath);
        String name = getFilenameFromPath(fullToPath);

        if (isDirectory(fullFromPath)) {
            return new ResourceInfo(parentPath, name, null, ResourceType.DIRECTORY);
        }

        return new ResourceInfo(parentPath, name, stat.size(), ResourceType.FILE);
    }

    public ByteArrayResource downloadResource(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);

        if (isDirectory(path)) {
            return downloadFolderAsZip(path, id);
        } else {
            return downloadFile(path, id);
        }
    }

    private ByteArrayResource downloadFile(String path, Integer id) {
        String fullPath = getFullPathById(path, id);

        try (InputStream stream = minioService.getInputStream(bucketName, fullPath)) {
            byte[] fileContent = stream.readAllBytes();
            return new ByteArrayResource(fileContent);
        } catch (Exception e) {
            throw new RuntimeException("Folder is empty or doesn't exist");
        }
    }

    private ByteArrayResource downloadFolderAsZip(String path, Integer id) {
        String fullPath = getFullPathById(path, id);
        String basePath = getFullPathById("", id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {
            Iterable<Result<Item>> results = minioService.searchResource(bucketName, fullPath);
            List<Item> items = getItemsListFromDirectory(results);

            if (items.isEmpty()) {
                throw new MinioException("Folder is empty or doesn't exist");
            }

            pushItemsToZip(items, basePath, zos);
        } catch (IOException | MinioException e) {
            throw new RuntimeException("Failed to process ZIP archive");
        }

        return new ByteArrayResource(baos.toByteArray());
    }

    private void pushItemsToZip(List<Item> items, String basePath, ZipArchiveOutputStream zos) throws ZipException, MinioException {
        for (Item item : items) {
            if (!item.isDir()) {
                String relativePath = item.objectName().substring(basePath.length());
                ZipArchiveEntry entry = new ZipArchiveEntry(relativePath);

                try {
                    zos.putArchiveEntry(entry);
                } catch (IOException e) {
                    throw new ZipException("Unexpected error while processing ZIP");
                }

                try (InputStream inputStream = minioService.getInputStream(bucketName, item.objectName())) {
                    inputStream.transferTo(zos);
                } catch (Exception e) {
                    throw new MinioException("MinIO operation failed");
                }

                try {
                    zos.closeArchiveEntry();
                } catch (IOException e) {
                    throw new ZipException("Failed to process ZIP archive");
                }
            }
        }
    }

    private List<Item> getItemsListFromDirectory(Iterable<Result<Item>> results) {
        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get();
                    } catch (ErrorResponseException e) {
                        throw new RuntimeException("MinIO error: " + e.errorResponse().message(), e);
                    } catch (MinioException e) {
                        throw new RuntimeException("MinIO operation failed: " + e.getMessage(), e);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get item: " + e.getMessage(), e);
                    }
                })
                .toList();
    }

    public void deleteResource(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);
        String fullPath = getFullPathById(path, id);

        minioService.getStatistics(bucketName, fullPath);

        if (isDirectory(path)) {
            String directoryPath = setDirectoryLink(fullPath);
            Iterable<Result<Item>> results = minioService.searchResource(bucketName, directoryPath);

            List<String> objectsToDelete = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get().objectName();
                        } catch (Exception e) {
                            throw new RuntimeException("MinIO operation failed");
                        }
                    })
                    .toList();

            objectsToDelete.forEach(objectName -> minioService.removeObject(bucketName, objectName));
        } else {
            minioService.removeObject(bucketName, fullPath);
        }
    }

    public ResourceInfo getResourceInfo(String encodedPath, Integer id) {
        String path = decodeString(encodedPath);

        if (isDirectory(path)) {
            return getDirectoryInfo(path, id);
        } else {
            return getFileInfo(path, id);
        }
    }

    private ResourceInfo getFileInfo(String filePath, Integer id) {
        String fullPath = getFullPathById(filePath, id);
        String parentPath = getParentPath(filePath);
        String fileName = getFilenameFromPath(filePath);

        StatObjectResponse stat = minioService.getStatistics(bucketName, fullPath);

        return new ResourceInfo(parentPath, fileName, stat.size(), ResourceType.FILE);
    }

    private ResourceInfo getDirectoryInfo(String directoryPath, Integer id) {
        String fullPath = getFullPathById(directoryPath, id);
        boolean exists = minioService.isDirectoryExists(bucketName, fullPath);

        if (!exists) {
            throw new RuntimeException("Directory not found");
        }

        String parentPath = getParentPath(directoryPath);
        String directoryName = getFilenameFromPath(directoryPath);

        return new ResourceInfo(parentPath, directoryName + "/", null, ResourceType.DIRECTORY);
    }
}
