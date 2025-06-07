package com.cloud.storage.service.resource;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    public void putObject(String bucketName, String path) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to put object: " + path, e);
        }
    }

    public boolean isDirectoryExists(String bucketName, String path) {
        return minioClient.listObjects(
                        ListObjectsArgs.builder()
                                .bucket(bucketName)
                                .prefix(path)
                                .maxKeys(1)
                                .build())
                .iterator().hasNext();
    }

    public Iterable<Result<Item>> getDirectoryContent(String bucketName, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .recursive(false)
                        .delimiter("/")
                        .build());
    }

    @Async("fileUploadExecutor")
    public CompletableFuture<Void> uploadResource(String bucketName, String path, MultipartFile file) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload resource: " + path, e);
        }
    }

    public StatObjectResponse getStatistics(String bucketName, String path) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to stat object: " + path, e);
        }
    }

    public Iterable<Result<Item>> searchResource(String bucketName, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .recursive(true)
                        .build());
    }

    public void copyObject(String bucketName, String fromPath, String toPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(bucketName)
                            .object(toPath)
                            .source(CopySource.builder()
                                    .bucket(bucketName)
                                    .object(fromPath)
                                    .build())
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy object: " + fromPath, e);
        }
    }

    public void removeObject(String bucketName, String path) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove object: " + path, e);
        }
    }

    public InputStream getInputStream(String bucketName, String path) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(path)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get object: " + path, e);
        }
    }

    public boolean isExist(String bucketName) {
        try {
            return minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if bucket exists: " + bucketName, e);
        }
    }

    public void makeBucket(String bucketName) {
        try {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to make bucket: " + bucketName, e);
        }
    }
}
