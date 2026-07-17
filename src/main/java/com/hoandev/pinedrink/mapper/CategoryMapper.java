package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Category;
import com.hoandev.pinedrink.entity.dto.request.Category.CreateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryOptionResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategorySummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public CategorySummaryResponse toSummaryResponse(Category category) {
        if (category == null) {
            return null;
        }

        return CategorySummaryResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .status(category.getStatus())
                .build();
    }

    public CategoryOptionResponse toOptionResponse(Category category) {
        if (category == null) {
            return null;
        }

        return CategoryOptionResponse.builder()
                .id(category.getId())
                .code(category.getCode())
                .name(category.getName())
                .imageUrl(category.getImageUrl())
                .build();
    }

    public Category toEntity(CreateCategoryRequest request) {
        if (request == null) {
            return null;
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        return category;
    }

    public void updateEntity(Category category, UpdateCategoryRequest request) {
        if (category == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }
    }
}
