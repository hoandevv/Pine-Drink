package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Category;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.enums.CategoryStatus;
import com.hoandev.pinedrink.entity.enums.FileVisibility;
import com.hoandev.pinedrink.entity.enums.ProductStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ProductMapper;
import com.hoandev.pinedrink.repository.CategoryRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.FileStorageService;
import com.hoandev.pinedrink.service.ProductService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        return create(request, null);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
        validateActiveCategory(category);
        String productCode = resolveCreateCode();
        Product product = productMapper.toEntity(request, category);
        String uploadedImageUrl = uploadProductImage(imageFile);
        if (uploadedImageUrl != null) {
            product.setImageUrl(uploadedImageUrl);
        }
        product.setCode(productCode);
        product.setStatus(ProductStatus.ACTIVE.getValue());
        try {
            product = productRepository.save(product);
            log.info("Product created: id={}, code={}", product.getId(), product.getCode());
            return productMapper.toResponse(product);
        } catch (RuntimeException ex) {
            deleteManagedProductImage(uploadedImageUrl);
            throw ex;
        }
    }

    @Override
    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request) {
        return update(id, request, null);
    }

    @Override
    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request, MultipartFile imageFile) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
        String oldImageUrl = product.getImageUrl();
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
            validateActiveCategory(category);
            product.setCategory(category);
        }
        productMapper.updateEntity(product, request);
        String uploadedImageUrl = uploadProductImage(imageFile);
        if (uploadedImageUrl != null) {
            product.setImageUrl(uploadedImageUrl);
        }
        String newImageUrl = product.getImageUrl();
        try {
            product = productRepository.save(product);
            deleteReplacedProductImage(oldImageUrl, newImageUrl);
            log.info("Product updated: id={}, code={}", product.getId(), product.getCode());
            return productMapper.toResponse(product);
        } catch (RuntimeException ex) {
            if (uploadedImageUrl != null) {
                deleteManagedProductImage(uploadedImageUrl);
            }
            throw ex;
        }
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(String id, UpdateProductStatusRequest request) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
        if (ProductStatus.ACTIVE.getValue().equals(request.getStatus().getValue())) {
            validateActiveCategory(product.getCategory());
        }
        product.setStatus(request.getStatus().getValue());
        product = productRepository.save(product);
        log.info("Product status updated: id={}, code={}, status={}", product.getId(), product.getCode(), product.getStatus());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
        if (ProductStatus.INACTIVE.getValue().equals(product.getStatus())) {
            throw new BaseException(ErrorCode.PRODUCT_005);
        }
        product.setStatus(ProductStatus.INACTIVE.getValue());
        productRepository.save(product);
        log.info("Product deleted (soft): id={}, code={}", product.getId(), product.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        return productMapper.toResponse(getProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(String keyword, String categoryId, String status, Pageable pageable) {
        Page<Product> products = productRepository.searchProducts(
                keyword,
                categoryId,
                status,
                ProductStatus.ACTIVE.getValue().equals(status) ? ProductStatus.ACTIVE.getValue() : null,
                pageable);
        List<ProductResponse> content = products.getContent().stream()
                .map(productMapper::toResponse)
                .toList();
        return PageResponse.from(products, content);
    }

    private Product getProductOrThrow(String id) {
        return productRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private String resolveCreateCode() {
        String generatedCode = codeGenerator.generate("PR", "PRODUCT");
        if (productRepository.existsByCode(generatedCode)) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }
        return generatedCode;
    }

    private void validateActiveCategory(Category category) {
        if (!CategoryStatus.ACTIVE.getValue().equals(category.getStatus())) {
            throw new BaseException(ErrorCode.PRODUCT_004);
        }
    }

    private String uploadProductImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }
        return fileStorageService.uploadFile(imageFile, "products", FileVisibility.PUBLIC);
    }

    private void deleteReplacedProductImage(String oldImageUrl, String newImageUrl) {
        if (oldImageUrl == null || oldImageUrl.isBlank()) {
            return;
        }
        if (oldImageUrl.equals(newImageUrl)) {
            return;
        }
        deleteManagedProductImage(oldImageUrl);
    }

    private void deleteManagedProductImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        if (!isManagedProductImage(imageUrl)) {
            return;
        }
        fileStorageService.deleteFile(imageUrl);
    }

    private boolean isManagedProductImage(String imageUrl) {
        return imageUrl.startsWith("products/") || imageUrl.contains("/products/");
    }
}
