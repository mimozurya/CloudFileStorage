package com.cloud.storage.services.resource;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

@RequiredArgsConstructor
@Service
public class MinioService {

    private final MinioClient minioClient;

    public void putObject(String bucketName, String path) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build());
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

    public void uploadResource(String bucketName, String path, MultipartFile file) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build());
    }

    public StatObjectResponse getStatistics(String bucketName, String path) throws Exception {
        return minioClient.statObject(
                StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build());
    }

    public Iterable<Result<Item>> searchResource(String bucketName, String path) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(path)
                        .recursive(true)
                        .build());
    }

    public void copyObject(String bucketName, String fromPath, String toPath) throws Exception {
        minioClient.copyObject(
                CopyObjectArgs.builder()
                        .bucket(bucketName)
                        .object(toPath)
                        .source(CopySource.builder()
                                .bucket(bucketName)
                                .object(fromPath)
                                .build())
                        .build());
    }

    public void removeObject(String bucketName, String path) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build());
    }

    public InputStream getInputStream(String bucketName, String path) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(path)
                        .build());
    }

    public boolean isBucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build());
    }

    public void makeBucket(String bucketName) throws Exception {
        minioClient.makeBucket(
                MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build());
    }
}
