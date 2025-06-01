package com.cloud.storage.services.resource;

import com.cloud.storage.config.minio.MinioConfigProperties;
import com.cloud.storage.dto.resource.ResourceInfo;
import com.cloud.storage.dto.resource.ResourceType;
import io.minio.Result;
import io.minio.StatObjectResponse;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static com.cloud.storage.utils.PathUtils.*;

@Service
public class FileService {
    private final String bucketName;
    private final MinioService minioService;

    public FileService(MinioConfigProperties minioConfig, MinioService minioService) {
        this.bucketName = minioConfig.getBucketName();
        this.minioService = minioService;
    }

    public void createUserBucket(Integer userId) throws Exception {
        String fullPath = getFullPath("", userId);

        boolean found = minioService.isBucketExists(bucketName);
        if (!found) {
            minioService.makeBucket(bucketName);
        }

        minioService.putObject(bucketName, fullPath);
    }

    public ResourceInfo createDirectory(String encodedPath, Integer id) throws Exception {
        String path = decodeString(encodedPath);
        String directoryPath = setDirectoryLink(path);
        String fullPath = getFullPath(directoryPath, id);

        if (isDirectoryExists(fullPath)) {
            throw new MinioException("Directory already exists");
        }

        minioService.putObject(bucketName, fullPath);

        return getDirectoryInfo(directoryPath, id);
    }

    private boolean isDirectoryExists(String path) throws Exception {
        try {
            return minioService.isDirectoryExists(bucketName, path);
        } catch (Exception e) {
            throw new MinioException("Failed to check directory existence");
        }
    }

    public List<ResourceInfo> getDirectoryContents(String encodedPath, Integer id) throws Exception {
        String path = decodeString(encodedPath);
        String directoryPath = path.replaceAll("/+$", "") + "/";
        String fullPath = getFullPath(directoryPath, id);

        List<ResourceInfo> contents = new ArrayList<>();
        Iterable<Result<Item>> results = minioService.getDirectoryContent(bucketName, fullPath);

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectPath = item.objectName().replace("user-" + id + "-files/", "");

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

        return contents;
    }

    public ResponseEntity<List<ResourceInfo>> uploadResource(String encodedPath, List<MultipartFile> files, Integer id) throws Exception {
        String path = decodeString(encodedPath);
        List<ResourceInfo> uploadedResources = new ArrayList<>();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();

            String normalizedBase = setDirectoryLink(path);
            String normalizedFilename = originalFilename.startsWith("/") ? originalFilename.substring(1) : originalFilename;
            String fullPath = getFullPath(normalizedBase + normalizedFilename, id);

            minioService.uploadResource(bucketName, fullPath, file);

            uploadedResources.add(createResourceInfo(normalizedBase + normalizedFilename, id));
        }

        return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(uploadedResources);
    }

    private ResourceInfo createResourceInfo(String path, Integer id) throws Exception {
        String parentPath = getParentPath(path);
        String name = getFilenameFromPath(path);
        String fullPath = getFullPath(path, id);

        if (isDirectory(path)) {
            return new ResourceInfo(parentPath, name, null, ResourceType.DIRECTORY);
        } else {
            StatObjectResponse stat = minioService.getStatistics(bucketName, fullPath);
            return new ResourceInfo(parentPath, name, stat.size(), ResourceType.FILE);
        }
    }

    public List<ResourceInfo> searchResource(String encodedQuery, Integer id) throws Exception {
        String query = decodeString(encodedQuery);

        Iterable<Result<Item>> results = minioService.searchResource(bucketName, query);

        List<ResourceInfo> foundResources = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectPath = item.objectName();

            if (item.isDir()) {
                foundResources.add(getDirectoryInfo(objectPath, id));
            } else {
                foundResources.add(getFileInfo(objectPath, id));
            }
        }

        return foundResources;
    }

    public ResourceInfo moveResource(String encodedFromPath, String encodedToPath, Integer id) throws Exception {
        String fullFromPath = getFullPath(decodeString(encodedFromPath), id);
        String fullToPath = getFullPath(decodeString(encodedToPath), id);

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

    public ResponseEntity<ByteArrayResource> downloadResource(String encodedPath, Integer id) throws Exception {
        String path = decodeString(encodedPath);

        if (isDirectory(path)) {
            return downloadFolderAsZip(path, id);
        } else {
            return downloadFile(path, id);
        }
    }

    private ResponseEntity<ByteArrayResource> downloadFile(String path, Integer id) throws Exception {
        String fullPath = getFullPath(path, id);

        try (InputStream stream = minioService.getInputStream(bucketName, fullPath)) {
            byte[] fileContent = stream.readAllBytes();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + getFilenameFromPath(path) + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new ByteArrayResource(fileContent));
        }
    }

    private ResponseEntity<ByteArrayResource> downloadFolderAsZip(String path, Integer id) throws Exception {
        String fullPath = getFullPath(path, id);
        String basePath = getFullPath("", id);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(baos)) {
            Iterable<Result<Item>> results = minioService.searchResource(bucketName, fullPath);

            List<Item> items = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            if (items.isEmpty()) {
                throw new MinioException("Folder is empty or doesn't exist");
            }

            for (Item item : items) {
                if (!item.isDir()) {
                    String relativePath = item.objectName().substring(basePath.length());
                    ZipArchiveEntry entry = new ZipArchiveEntry(relativePath);
                    zos.putArchiveEntry(entry);

                    try (InputStream inputStream = minioService.getInputStream(bucketName, item.objectName())) {
                        inputStream.transferTo(zos);
                    }

                    zos.closeArchiveEntry();
                }
            }
        }
        String zipName = getFilenameFromPath(path) + ".zip";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + zipName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new ByteArrayResource(baos.toByteArray()));
    }

    public void deleteResource(String encodedPath, Integer id) throws Exception {
        String path = decodeString(encodedPath);
        String fullPath = getFullPath(path, id);

        try {
            minioService.getStatistics(bucketName, fullPath);
        } catch (Exception e) {
            throw new MinioException("Resource not found");
        }

        if (isDirectory(path)) {
            String directoryPath = setDirectoryLink(fullPath);
            Iterable<Result<Item>> results = minioService.searchResource(bucketName, directoryPath);

            List<String> objectsToDelete = StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get().objectName();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .toList();

            for (String objectName : objectsToDelete) {
                minioService.removeObject(bucketName, objectName);
            }
        } else {
            minioService.removeObject(bucketName, fullPath);
        }
    }

    public ResourceInfo getResourceInfo(String encodedPath, Integer id) throws Exception {
        String path = decodeString(encodedPath);

        if (isDirectory(path)) {
            return getDirectoryInfo(path, id);
        } else {
            return getFileInfo(path, id);
        }
    }

    private ResourceInfo getFileInfo(String filePath, Integer id) throws Exception {
        String fullPath = getFullPath(filePath, id);
        StatObjectResponse stat = minioService.getStatistics(bucketName, fullPath);

        String parentPath = getParentPath(filePath);
        String fileName = getFilenameFromPath(filePath);

        return new ResourceInfo(parentPath, fileName, stat.size(), ResourceType.FILE);
    }

    private ResourceInfo getDirectoryInfo(String directoryPath, Integer id) throws Exception {
        String fullPath = getFullPath(directoryPath, id);
        boolean exists = minioService.isDirectoryExists(bucketName, fullPath);

        if (!exists) {
            throw new MinioException("Directory not found");
        }

        String parentPath = getParentPath(directoryPath);
        String directoryName = getFilenameFromPath(directoryPath);

        return new ResourceInfo(parentPath, directoryName + "/", null, ResourceType.DIRECTORY);
    }
}
