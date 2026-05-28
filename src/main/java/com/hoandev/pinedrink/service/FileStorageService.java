package com.hoandev.pinedrink.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Service interface for file storage operations using MinIO.
 */
public interface FileStorageService {

    /**
     * Uploads a file to MinIO storage.
     *
     * @param file the file to upload
     * @param folder the folder path in the bucket (e.g., "avatars", "products")
     * @return the public URL of the uploaded file
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Deletes a file from MinIO storage.
     *
     * @param fileUrl the public URL of the file to delete
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
