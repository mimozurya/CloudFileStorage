package com.cloud.storage.config.minio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class MinioConfigProperties {
    private String url;
    private String accessKey;
    private String secretKey;
    private String bucketName;
}
