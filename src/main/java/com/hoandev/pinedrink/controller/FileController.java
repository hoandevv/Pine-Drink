package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.enums.FileVisibility;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;

/**
 * REST controller for file operations.
 * Handles proxying private files from MinIO storage.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private final FileStorageService fileStorageService;

    /**
     * Proxies private files from MinIO storage.
     * This endpoint allows authenticated users to access private files.
     *
     * @param folder the folder name (e.g., "invoices", "exports")
     * @param filename the file name
     * @return the file as a stream
     */
    @GetMapping("/private/{folder}/{filename}")
    public ResponseEntity<InputStreamResource> getPrivateFile(
            @PathVariable String folder,
            @PathVariable String filename) {
        
        try {
            String objectName = folder + "/" + filename;
            log.info("Fetching private file: {}", objectName);

            // Get file stream from MinIO
            InputStream fileStream = fileStorageService.getFileStream(objectName, FileVisibility.PRIVATE);

            // Determine content type based on file extension
            String contentType = determineContentType(filename);

            // Return file as stream
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .body(new InputStreamResource(fileStream));

        } catch (Exception e) {
            log.error("Failed to fetch private file: {}/{}", folder, filename, e);
            throw new BaseException(ErrorCode.COM_002);
        }
    }

    /**
     * Determines content type based on file extension.
     */
    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "xls" -> "application/vnd.ms-excel";
            case "csv" -> "text/csv";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}
