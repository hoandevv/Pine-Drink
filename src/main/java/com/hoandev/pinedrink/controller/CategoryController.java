package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Category.CreateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryOptionResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategorySummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.service.CategoryService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
/**
 * Controller for managing categories.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PERM_CATEGORY_CREATE')")
    public ResponseEntity<BaseResponse<CategoryResponse>> create(@Valid @RequestBody CreateCategoryRequest request) {
        log.info("Creating category: name={}", request.getName());
        CategoryResponse response = categoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Category created successfully"));
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_CATEGORY_CREATE')")
    public ResponseEntity<BaseResponse<CategoryResponse>> createWithImage(
            @Valid @RequestPart("request") CreateCategoryRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Creating category with image: name={}", request.getName());
        CategoryResponse response = categoryService.create(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Category created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATEGORY_UPDATE')")
    public ResponseEntity<BaseResponse<CategoryResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        log.info("Updating category: id={}", id);
        CategoryResponse response = categoryService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Category updated successfully"));
    }

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('PERM_CATEGORY_UPDATE')")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateWithImage(
            @PathVariable String id,
            @Valid @RequestPart("request") UpdateCategoryRequest request,
            @RequestPart(value = "file", required = false) MultipartFile file) {
        log.info("Updating category with image: id={}", id);
        CategoryResponse response = categoryService.update(id, request, file);
        return ResponseEntity.ok(BaseResponse.success(response, "Category updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('PERM_CATEGORY_UPDATE')")
    public ResponseEntity<BaseResponse<CategoryResponse>> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateCategoryStatusRequest request) {
        log.info("Updating category status: id={}, status={}", id, request.getStatus());
        CategoryResponse response = categoryService.updateStatus(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Category status updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PERM_CATEGORY_DELETE')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        log.info("Deleting category: id={}", id);
        categoryService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Category deleted successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BaseResponse<CategoryResponse>> getById(@PathVariable String id) {
        log.info("Getting category: id={}", id);
        CategoryResponse response = categoryService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response, "Category retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<CategorySummaryResponse>>> getAll(
            @PageableDefault(size = 20, sort = "displayOrder", direction = Sort.Direction.ASC) Pageable pageable) {
        log.info("Getting categories");
        PageResponse<CategorySummaryResponse> response = categoryService.getSummaries(pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Categories retrieved successfully"));
    }

    @GetMapping("/active")
    public ResponseEntity<BaseResponse<List<CategoryOptionResponse>>> getAllActive() {
        log.info("Getting active categories");
        List<CategoryOptionResponse> response = categoryService.getActiveOptions();
        return ResponseEntity.ok(BaseResponse.success(response, "Active categories retrieved successfully"));
    }
}
