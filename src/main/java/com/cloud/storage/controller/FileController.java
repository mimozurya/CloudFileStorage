package com.cloud.storage.controller;

import com.cloud.storage.annotation.DirectoryNotExists;
import com.cloud.storage.config.security.PersonDetails;
import com.cloud.storage.dto.resource.ResourceInfo;
import com.cloud.storage.service.resource.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
@Tag(name = "File Controller", description = "A controller for working with o3 data")
public class FileController {
    private final FileService fileService;

    @Operation(summary = "Get resource", description = "Returns resource info")
    @GetMapping("/resource")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo getResource(@RequestParam String path,
                                    @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.getResourceInfo(path, personDetails.getUserId());
    }

    @Operation(summary = "Delete resource", description = "Returns HTTP-status 'no content'")
    @DeleteMapping("/resource")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam String path,
                       @AuthenticationPrincipal PersonDetails personDetails) {
        fileService.deleteResource(path, personDetails.getUserId());
    }

    @Operation(summary = "Download resource", description = "Returns download file or zip")
    @GetMapping("/resource/download")
    @ResponseStatus(HttpStatus.OK)
    public ByteArrayResource download(@RequestParam String path,
                                      @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.downloadResource(path, personDetails.getUserId());
    }

    @Operation(summary = "Move or rename resource", description = "Returns new resource info")
    @GetMapping("/resource/move")
    @ResponseStatus(HttpStatus.OK)
    public ResourceInfo move(@RequestParam String from,
                             @RequestParam String to,
                             @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.moveResource(from, to, personDetails.getUserId());
    }

    @Operation(summary = "Search resource", description = "Retrieves a list of resource info")
    @GetMapping("/resource/search")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> search(@RequestParam String query,
                                     @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.searchResource(query, personDetails.getUserId());
    }

    @Operation(summary = "Upload resource", description = "Retrieves a list of resource info")
    @PostMapping(value = "/resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public List<ResourceInfo> upload(@RequestParam String path,
                                     @RequestPart("object") @Valid @Size(min = 1, max = 5, message = "You can upload 1 to 5 files at once") List<MultipartFile> files,
                                     @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.uploadResource(path, files, personDetails.getUserId());
    }

    @Operation(summary = "Get directory", description = "Returns a list of directories info")
    @GetMapping("/directory")
    @ResponseStatus(HttpStatus.OK)
    public List<ResourceInfo> getDirectory(@RequestParam String path,
                                           @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.getDirectoryContents(path, personDetails.getUserId());
    }

    @Operation(summary = "Create directory", description = "Returns a directory info")
    @PostMapping("/directory")
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceInfo createDirectory(@RequestParam @DirectoryNotExists String path,
                                        @AuthenticationPrincipal PersonDetails personDetails) {
        return fileService.createDirectory(path, personDetails.getUserId());
    }
}
