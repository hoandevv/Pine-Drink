package com.hoandev.pinedrink.entity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for file upload operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    /**
     * The public URL of the uploaded file.
     */
    private String fileUrl;

    /**
     * The original filename.
     */
    private String originalFilename;

    /**
     * The file size in bytes.
     */
    private long fileSize;

    /**
     * The content type of the file.
     */
    private String contentType;

}
