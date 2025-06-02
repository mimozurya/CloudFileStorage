package com.cloud.storage.controllers;

import com.cloud.storage.config.security.PersonDetails;
import com.cloud.storage.dto.resource.ResourceInfo;
import com.cloud.storage.services.resource.FileService;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "Get resource", description = "Returns resource info")
    @GetMapping("/resource")
    public ResourceInfo getResource(@RequestParam String path,
                                    @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.getResourceInfo(path, personDetails.getUserId());
    }

    @Operation(summary = "Delete resource", description = "Returns HTTP-status 'no content'")
    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path,
                       @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        fileService.deleteResource(path, personDetails.getUserId());
    }

    @Operation(summary = "Download resource", description = "Returns download file or zip")
    @GetMapping("/resource/download")
    public ResponseEntity<?> download(@RequestParam String path,
                                      @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.downloadResource(path, personDetails.getUserId());
    }

    @Operation(summary = "Move or rename resource", description = "Returns new resource info")
    @GetMapping("/resource/move")
    public ResourceInfo move(@RequestParam String from,
                             @RequestParam String to,
                             @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.moveResource(from, to, personDetails.getUserId());
    }

    @Operation(summary = "Search resource", description = "Returns a list of resource info")
    @GetMapping("/resource/search")
    public List<ResourceInfo> search(@RequestParam String query,
                                     @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.searchResource(query, personDetails.getUserId());
    }

    @Operation(summary = "Upload resource", description = "Returns a list of resource info")
    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<ResourceInfo>> upload(@RequestParam String path,
                                                     @RequestPart("object") List<MultipartFile> files,
                                                     @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.uploadResource(path, files, personDetails.getUserId());
    }

    @Operation(summary = "Get directory", description = "Returns a list of directories info")
    @GetMapping("/directory")
    public List<ResourceInfo> getDirectory(@RequestParam String path,
                                           @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.getDirectoryContents(path, personDetails.getUserId());
    }

    @Operation(summary = "Create directory", description = "Returns a directory info")
    @PostMapping("/directory")
    public ResourceInfo createDirectory(@RequestParam String path,
                                        @AuthenticationPrincipal PersonDetails personDetails) throws Exception {
        return fileService.createDirectory(path, personDetails.getUserId());
    }
}
