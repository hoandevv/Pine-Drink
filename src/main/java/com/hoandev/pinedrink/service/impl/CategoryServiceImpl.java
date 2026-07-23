package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Category;
import com.hoandev.pinedrink.entity.dto.request.Category.CreateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryRequest;
import com.hoandev.pinedrink.entity.dto.request.Category.UpdateCategoryStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryOptionResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategoryResponse;
import com.hoandev.pinedrink.entity.dto.response.Category.CategorySummaryResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.enums.CategoryStatus;
import com.hoandev.pinedrink.entity.enums.FileVisibility;
import com.hoandev.pinedrink.entity.enums.ProductStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.CategoryMapper;
import com.hoandev.pinedrink.repository.CategoryRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.CategoryService;
import com.hoandev.pinedrink.service.FileStorageService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;
    private final FileStorageService fileStorageService;

    public CategoryServiceImpl(CategoryRepository categoryRepository, ProductRepository productRepository, CategoryMapper categoryMapper, AccessScopeService accessScopeService, CodeGenerator codeGenerator, FileStorageService fileStorageService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
        this.accessScopeService = accessScopeService;
        this.codeGenerator = codeGenerator;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        String categoryCode = resolveCreateCode();
        Category category = categoryMapper.toEntity(request);
        String uploadedImageUrl = uploadCategoryImage(imageFile);
        if (uploadedImageUrl != null) {
            category.setImageUrl(uploadedImageUrl);
        }
        category.setCode(categoryCode);
        category.setStatus(CategoryStatus.ACTIVE.getValue());
        try {
            category = categoryRepository.save(category);
            log.info("Category created: id={}, code={}", category.getId(), category.getCode());
            return categoryMapper.toResponse(category);
        } catch (RuntimeException ex) {
            deleteManagedCategoryImage(uploadedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public CategoryResponse update(String id, UpdateCategoryRequest request) {
        return update(id, request, null);
    }

    @Override
    @Transactional
    public CategoryResponse update(String id, UpdateCategoryRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        Category category = getCategoryOrThrow(id);
        String oldImageUrl = category.getImageUrl();
        categoryMapper.updateEntity(category, request);
        String uploadedImageUrl = uploadCategoryImage(imageFile);
        if (uploadedImageUrl != null) {
            category.setImageUrl(uploadedImageUrl);
        }
        String newImageUrl = category.getImageUrl();
        try {
            category = categoryRepository.save(category);
            deleteReplacedCategoryImage(oldImageUrl, newImageUrl);
            log.info("Category updated: id={}, code={}", category.getId(), category.getCode());
            return categoryMapper.toResponse(category);
        } catch (RuntimeException ex) {
            if (uploadedImageUrl != null) {
                deleteManagedCategoryImage(uploadedImageUrl);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public CategoryResponse updateStatus(String id, UpdateCategoryStatusRequest request) {
        accessScopeService.assertSystemAccess();
        Category category = getCategoryOrThrow(id);
        String oldStatus = category.getStatus();
        category.setStatus(request.getStatus().getValue());
        category = categoryRepository.save(category);
        int affectedProducts = syncProductsByCategoryStatus(category, oldStatus);
        log.info("Category status updated: id={}, code={}, status={}", category.getId(), category.getCode(), category.getStatus());
        if (affectedProducts > 0) {
            log.info("Products synced by category status update: categoryId={}, oldStatus={}, newStatus={}, count={}",
                    category.getId(), oldStatus, category.getStatus(), affectedProducts);
        }
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertSystemAccess();
        Category category = getCategoryOrThrow(id);
        if (CategoryStatus.INACTIVE.getValue().equals(category.getStatus())) {
            throw new BaseException(ErrorCode.CATEGORY_003);
        }
        category.setStatus(CategoryStatus.INACTIVE.getValue());
        categoryRepository.save(category);
        int affectedProducts = updateProductsByCategory(
                category.getId(),
                ProductStatus.ACTIVE.getValue(),
                ProductStatus.INACTIVE.getValue());
        log.info("Category deleted (soft): id={}, code={}", category.getId(), category.getCode());
        if (affectedProducts > 0) {
            log.info("Products inactivated by category delete: categoryId={}, count={}", category.getId(), affectedProducts);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {
        return categoryMapper.toResponse(getCategoryOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CategorySummaryResponse> getSummaries(Pageable pageable) {
        Page<Category> categories = categoryRepository.findAll(pageable);
        List<CategorySummaryResponse> content = categories.getContent().stream().map(categoryMapper::toSummaryResponse).toList();
        return PageResponse.from(categories, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryOptionResponse> getActiveOptions() {
        return categoryRepository.findByStatusOrderByDisplayOrder(CategoryStatus.ACTIVE.getValue())
                .stream()
                .map(categoryMapper::toOptionResponse)
                .toList();
    }

    private Category getCategoryOrThrow(String id) {
        return categoryRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.CATEGORY_001));
    }

    private String resolveCreateCode() {
        String generatedCode = codeGenerator.generate("CA", "CATEGORY");
        if (categoryRepository.existsByCode(generatedCode)) {
            throw new BaseException(ErrorCode.CATEGORY_002);
        }
        return generatedCode;
    }

    private String uploadCategoryImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        return fileStorageService.uploadFile(imageFile, "categories", FileVisibility.PUBLIC);
    }

    private void deleteReplacedCategoryImage(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }
        if (oldImageUrl.equals(newImageUrl)) {
            return;
        }
        deleteManagedCategoryImage(oldImageUrl);
    }

    private void deleteManagedCategoryImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (!isManagedCategoryImage(imageUrl)) {
            return;
        }
        fileStorageService.deleteFile(imageUrl);
    }

    private boolean isManagedCategoryImage(String imageUrl) {
        return imageUrl.startsWith("categories/") || imageUrl.contains("/categories/");
    }

    private int syncProductsByCategoryStatus(Category category, String oldStatus) {
        String newStatus = category.getStatus();
        if (oldStatus.equals(newStatus)) {
            return 0;
        }
        if (CategoryStatus.INACTIVE.getValue().equals(newStatus)) {
            return updateProductsByCategory(
                    category.getId(),
                    ProductStatus.ACTIVE.getValue(),
                    ProductStatus.INACTIVE.getValue());
        }
        if (CategoryStatus.ACTIVE.getValue().equals(newStatus)) {
            return updateProductsByCategory(
                    category.getId(),
                    ProductStatus.INACTIVE.getValue(),
                    ProductStatus.ACTIVE.getValue());
        }
        return 0;
    }

    private int updateProductsByCategory(String categoryId, String currentStatus, String productStatus) {
        return productRepository.updateStatusByCategoryIdAndStatus(categoryId, currentStatus, productStatus);
    }
}
