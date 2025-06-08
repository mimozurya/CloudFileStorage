package com.cloud.storage.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class PathUtils {
    public static String decodeString(String encodedPath) {
        return URLDecoder.decode(encodedPath, StandardCharsets.UTF_8);
    }

    public static String getFullPathById(String path, Integer id) {
        String normalizedPath = path.replaceAll("^/+", "");
        return String.format("user-%d-files/%s", id, normalizedPath);
    }

    public static Boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    public static String getFilenameFromPath(String path) {
        String trimmedPath = path.replaceAll("/+$", "");
        int lastSlash = trimmedPath.lastIndexOf('/');
        return lastSlash >= 0 ? trimmedPath.substring(lastSlash + 1) : trimmedPath;
    }

    public static String getParentPath(String path) {
        String normalizedPath = path.replaceAll("/+$", "");
        int lastSlash = normalizedPath.lastIndexOf('/');

        if (lastSlash <= 0) {
            return "/";
        } else {
            return "/" + normalizedPath.substring(0, lastSlash + 1);
        }
    }

    public static String setDirectoryLink(String path) {
        return path.endsWith("/") ? path : path + "/";
    }
}
