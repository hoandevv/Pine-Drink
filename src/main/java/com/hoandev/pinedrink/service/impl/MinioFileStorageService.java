package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.MinioProperties;
import com.hoandev.pinedrink.entity.enums.FileVisibility;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.FileStorageService;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Implementation of {@link FileStorageService} using MinIO.
 */
@Service
@Slf4j
public class MinioFileStorageService implements FileStorageService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public MinioFileStorageService(MinioClient minioClient, MinioProperties minioProperties) {
        this.minioClient = minioClient;
        this.minioProperties = minioProperties;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String uploadFile(MultipartFile file, String folder, FileVisibility visibility) {
        validateFile(file);

        try {
            // Determine bucket based on visibility
            String bucketName = getBucketName(visibility);
            
            // Ensure bucket exists
            ensureBucketExists(bucketName, visibility);

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = getFileExtension(originalFilename);
            String filename = UUID.randomUUID().toString() + "." + extension;
            String objectName = folder + "/" + filename;

            // Upload file to MinIO
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(objectName)
                                .stream(inputStream, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }

            log.info("File uploaded successfully: {} to bucket: {}", objectName, bucketName);

            // Return URL based on visibility
            return buildUrl(objectName, visibility);

        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new BaseException(ErrorCode.COM_002);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getFileStream(String objectName, FileVisibility visibility) {
        try {
            String bucketName = getBucketName(visibility);
            
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to get file stream from MinIO: {}", objectName, e);
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

            // Determine bucket from URL
            String bucketName = extractBucketNameFromUrl(fileUrl);
            if (bucketName == null) {
                log.warn("Cannot extract bucket name from URL: {}", fileUrl);
                return;
            }

            // Delete file from MinIO
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("File deleted successfully: {} from bucket: {}", objectName, bucketName);

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
     * Gets bucket name based on visibility.
     */
    private String getBucketName(FileVisibility visibility) {
        return visibility == FileVisibility.PUBLIC 
                ? minioProperties.getPublicBucketName() 
                : minioProperties.getPrivateBucketName();
    }

    /**
     * Ensures the bucket exists, creates it if not.
     */
    private void ensureBucketExists(String bucketName, FileVisibility visibility) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info("Bucket created: {}", bucketName);
        }

        // Always set bucket policy for public buckets to ensure it's public
        if (visibility == FileVisibility.PUBLIC) {
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
                    """.formatted(bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );

            log.info("Bucket policy set to public: {}", bucketName);
        }
    }

    /**
     * Builds the URL for accessing the file based on visibility.
     */
    private String buildUrl(String objectName, FileVisibility visibility) {
        if (visibility == FileVisibility.PUBLIC) {
            // Public files: direct MinIO URL
            String bucketName = minioProperties.getPublicBucketName();
            if (minioProperties.getPublicUrl() != null && !minioProperties.getPublicUrl().isEmpty()) {
                return minioProperties.getPublicUrl() + "/" + bucketName + "/" + objectName;
            }
            return minioProperties.getEndpoint() + "/" + bucketName + "/" + objectName;
        } else {
            // Private files: return object name only (will be proxied through backend)
            return objectName;
        }
    }

    /**
     * Extracts object name from the URL.
     */
    private String extractObjectNameFromUrl(String fileUrl) {
        try {
            // Try public bucket
            String publicBucket = minioProperties.getPublicBucketName();
            int publicIndex = fileUrl.indexOf(publicBucket);
            if (publicIndex != -1) {
                return fileUrl.substring(publicIndex + publicBucket.length() + 1);
            }

            // Try private bucket
            String privateBucket = minioProperties.getPrivateBucketName();
            int privateIndex = fileUrl.indexOf(privateBucket);
            if (privateIndex != -1) {
                return fileUrl.substring(privateIndex + privateBucket.length() + 1);
            }

            // Try legacy bucket
            String legacyBucket = minioProperties.getBucketName();
            int legacyIndex = fileUrl.indexOf(legacyBucket);
            if (legacyIndex != -1) {
                return fileUrl.substring(legacyIndex + legacyBucket.length() + 1);
            }

            // If no bucket found, assume it's just the object name
            return fileUrl;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts bucket name from the URL.
     */
    private String extractBucketNameFromUrl(String fileUrl) {
        try {
            if (fileUrl.contains(minioProperties.getPublicBucketName())) {
                return minioProperties.getPublicBucketName();
            }
            if (fileUrl.contains(minioProperties.getPrivateBucketName())) {
                return minioProperties.getPrivateBucketName();
            }
            if (fileUrl.contains(minioProperties.getBucketName())) {
                return minioProperties.getBucketName();
            }
            // Default to public bucket
            return minioProperties.getPublicBucketName();
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
