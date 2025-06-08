package com.cloud.storage.validator;

import com.cloud.storage.annotation.DirectoryNotExists;
import com.cloud.storage.config.minio.MinioConfigProperties;
import com.cloud.storage.service.resource.MinioService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DirectoryNotExistsValidator implements ConstraintValidator<DirectoryNotExists, String> {

    private final String bucketName;
    private final MinioService minioService;

    public DirectoryNotExistsValidator(MinioConfigProperties minioConfig, MinioService minioService) {
        this.bucketName = minioConfig.getBucketName();
        this.minioService = minioService;
    }

    @Override
    public void initialize(DirectoryNotExists constraintAnnotation) {
    }

    @Override
    public boolean isValid(String fullPath, ConstraintValidatorContext context) {
        if (fullPath == null) {
            return true;
        }

        try {
            return !minioService.isDirectoryExists(bucketName, fullPath);
        } catch (Exception e) {
            return false;
        }
    }
}
