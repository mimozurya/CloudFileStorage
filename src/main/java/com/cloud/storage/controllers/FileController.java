package com.cloud.storage.controllers;

import com.cloud.storage.config.security.PersonDetails;
import com.cloud.storage.dto.resource.ResourceInfo;
import com.cloud.storage.services.resource.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @GetMapping("/resource")
    public ResourceInfo getResource(@RequestParam String path,
                                    @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.getResourceInfo(path, personDetails.getUserId());
    }

    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path,
                       @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        fileService.deleteResource(path, personDetails.getUserId());
    }

    @GetMapping("/resource/download")
    public ResponseEntity<?> download(@RequestParam String path,
                                      @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.downloadResource(path, personDetails.getUserId());
    }

    @GetMapping("/resource/move")
    public ResourceInfo move(@RequestParam String from,
                             @RequestParam String to,
                             @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.moveResource(from, to, personDetails.getUserId());
    }

    @GetMapping("/resource/search")
    public List<ResourceInfo> search(@RequestParam String query,
                                     @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.searchResource(query, personDetails.getUserId());
    }

    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceInfo>> upload(@RequestParam String path,
                                                     @RequestPart("object") List<MultipartFile> files,
                                                     @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.uploadResource(path, files, personDetails.getUserId());
    }

    @GetMapping("/directory")
    public List<ResourceInfo> getDirectory(@RequestParam String path,
                                           @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.getDirectoryContents(path, personDetails.getUserId());
    }

    @PostMapping("/directory")
    public ResourceInfo createDirectory(@RequestParam String path,
                                        @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.createDirectory(path, personDetails.getUserId());
    }
}
