package com.businessos.shared.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FileUploadController {

    private final LocalFileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(toResponse(file, fileStorageService.storeFile(file)));
    }

    /** Profile-picture upload: images only, validated as real image data, 5MB cap. */
    @PostMapping("/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(toResponse(file, fileStorageService.storeAvatar(file)));
    }

    private Map<String, String> toResponse(MultipartFile file, String fileDownloadUri) {
        Map<String, String> response = new HashMap<>();
        response.put("fileName", file.getOriginalFilename());
        response.put("fileUrl", fileDownloadUri);
        response.put("message", "File uploaded successfully!");
        return response;
    }
}
