package com.hoandev.pinedrink.entity.dto.response.Category;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {

    private String id;
    private String code;
    private String name;
    private String description;
    private String imageUrl;
    private int displayOrder;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
