package com.hoandev.pinedrink.configuration.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for MinIO.
 */
@Configuration
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {

    /**
     * MinIO server endpoint URL.
     */
    private String endpoint;

    /**
     * MinIO access key.
     */
    private String accessKey;

    /**
     * MinIO secret key.
     */
    private String secretKey;

    /**
     * Default bucket name for storing files (deprecated - use publicBucketName or privateBucketName).
     */
    private String bucketName = "pine-drink";

    /**
     * Bucket name for public files (avatars, products, banners, logos).
     */
    private String publicBucketName = "pine-drink-public";

    /**
     * Bucket name for private files (invoices, exports, documents).
     */
    private String privateBucketName = "pine-drink-private";

    /**
     * Public URL for accessing files (if using reverse proxy/CDN).
     */
    private String publicUrl;

    /**
     * Presigned URL expiry time in seconds (default 1 hour).
     */
    private int presignedUrlExpiry = 3600;

    /**
     * Maximum file size in bytes (default 5MB).
     */
    private long maxFileSize = 5 * 1024 * 1024;

    /**
     * Allowed file extensions for upload.
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

}
