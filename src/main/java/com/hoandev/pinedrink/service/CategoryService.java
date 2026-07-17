package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Category.CreateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryOptionResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategorySummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse create(CreateCategoryRequest request, MultipartFile imageFile);

    CategoryResponse update(String id, UpdateCategoryRequest request);

    CategoryResponse update(String id, UpdateCategoryRequest request, MultipartFile imageFile);

    CategoryResponse updateStatus(String id, UpdateCategoryStatusRequest request);

    void delete(String id);

    CategoryResponse getById(String id);

    PageResponse<CategorySummaryResponse> getSummaries(Pageable pageable);

    List<CategoryOptionResponse> getActiveOptions();
}
