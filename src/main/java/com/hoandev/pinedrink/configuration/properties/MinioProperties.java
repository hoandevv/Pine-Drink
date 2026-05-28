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
     * Default bucket name for storing files.
     */
    private String bucketName = "pine-drink";

    /**
     * Public URL for accessing files (if using reverse proxy/CDN).
     */
    private String publicUrl;

    /**
     * Maximum file size in bytes (default 5MB).
     */
    private long maxFileSize = 5 * 1024 * 1024;

    /**
     * Allowed file extensions for upload.
     */
    private String[] allowedExtensions = {"jpg", "jpeg", "png", "gif", "webp"};

}
