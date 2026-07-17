package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.enums.FileVisibility;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Service interface for file storage operations using MinIO.
 */
public interface FileStorageService {

    /**
     * Uploads a file to MinIO storage.
     *
     * @param file the file to upload
     * @param folder the folder path in the bucket (e.g., "avatars", "products")
     * @param visibility the visibility level (PUBLIC or PRIVATE)
     * @return the URL of the uploaded file (direct URL for public, proxy URL for private)
     */
    String uploadFile(MultipartFile file, String folder, FileVisibility visibility);

    /**
     * Gets file stream from MinIO storage (for private files).
     *
     * @param objectName the object name (e.g., "invoices/uuid.pdf")
     * @param visibility the visibility level (PUBLIC or PRIVATE)
     * @return InputStream of the file
     */
    InputStream getFileStream(String objectName, FileVisibility visibility);

    /**
     * Deletes a file from MinIO storage.
     *
     * @param fileUrl the URL of the file to delete
     */
    void deleteFile(String fileUrl);

    /**
     * Validates if the file is allowed to be uploaded.
     *
     * @param file the file to validate
     * @throws com.hoandev.pinedrink.exception.BaseException if validation fails
     */
    void validateFile(MultipartFile file);

}
