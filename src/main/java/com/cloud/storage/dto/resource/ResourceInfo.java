package com.cloud.storage.dto.resource;

public record ResourceInfo(
        String path,
        String name,
        Long size,
        ResourceType type
) {
}
