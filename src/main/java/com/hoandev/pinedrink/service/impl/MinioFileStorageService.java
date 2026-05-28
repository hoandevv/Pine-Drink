package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.properties.MinioProperties;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.FileStorageService;
import io.minio.*;
import io.minio.errors.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Implementation of {@link FileStorageService} using MinIO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        try {
            // Ensure bucket exists
            ensureBucketExists();

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "." + extension;
            String objectName = folder + "/" + filename;

            // Upload file to MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.getBucketName())
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            log.info("File uploaded successfully: {}", objectName);

            // Return public URL
            return buildPublicUrl(objectName);

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BaseException(ErrorCode.COM_002);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return;
        }

        try {
            // Extract object name from URL
            String objectName = extractObjectNameFromUrl(fileUrl);
            if (objectName == null) {
                log.warn("Cannot extract object name from URL: {}", fileUrl);
                return;
            }

            // Delete file from MinIO
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .build()
            );

            log.info("File deleted successfully: {}", objectName);

        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", fileUrl, e);
            // Don't throw exception - deletion failure shouldn't block the main operation
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BaseException(ErrorCode.COM_004);
        }

        // Check file size
        if (file.getSize() > minioProperties.getMaxFileSize()) {
            throw new BaseException(ErrorCode.COM_004);
        }

        // Check file extension
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BaseException(ErrorCode.COM_004);
        }

        String extension = getFileExtension(filename).toLowerCase();
        boolean isAllowed = false;
        for (String allowedExt : minioProperties.getAllowedExtensions()) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                isAllowed = true;
                break;
            }
        }

        if (!isAllowed) {
            throw new BaseException(ErrorCode.COM_004);
        }
    }

    /**
     * Ensures the bucket exists, creates it if not.
     */
    private void ensureBucketExists() throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(minioProperties.getBucketName())
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .build()
            );

            // Set bucket policy to public read
            String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": {"AWS": "*"},
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(minioProperties.getBucketName());

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .config(policy)
                            .build()
            );

            log.info("Bucket created and policy set: {}", minioProperties.getBucketName());
        }
    }

    /**
     * Builds the public URL for accessing the file.
     */
    private String buildPublicUrl(String objectName) {
        if (minioProperties.getPublicUrl() != null && !minioProperties.getPublicUrl().isEmpty()) {
            return minioProperties.getPublicUrl() + "/" + minioProperties.getBucketName() + "/" + objectName;
        }
        return minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
    }

    /**
     * Extracts object name from the public URL.
     */
    private String extractObjectNameFromUrl(String fileUrl) {
        try {
            String bucketName = minioProperties.getBucketName();
            int bucketIndex = fileUrl.indexOf(bucketName);
            if (bucketIndex == -1) {
                return null;
            }
            return fileUrl.substring(bucketIndex + bucketName.length() + 1);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Gets file extension from filename.
     */
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

}
