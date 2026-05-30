package com.hoandev.pinedrink.entity.enums;

/**
 * Enum representing file visibility levels.
 */
public enum FileVisibility {
    /**
     * Public files - accessible directly from MinIO without authentication.
     * Examples: avatars, product images, banners, logos.
     */
    PUBLIC,

    /**
     * Private files - require authentication and authorization to access.
     * Examples: invoices, exports, documents.
     */
    PRIVATE
}
