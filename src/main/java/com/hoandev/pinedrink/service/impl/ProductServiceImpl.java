package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Brand;
import com.hoandev.pinedrink.entity.Category;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ProductMapper;
import com.hoandev.pinedrink.repository.BrandRepository;
import com.hoandev.pinedrink.repository.CategoryRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ProductService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import com.hoandev.pinedrink.utils.Constants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        accessScopeService.assertCanManageBrand(request.getBrandId());

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_003));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
        validateCategoryOwnership(category, brand.getId());

        String productCode = resolveCreateCode(request.getCode(), brand.getId());
        if (productRepository.existsByBrandIdAndCode(brand.getId(), productCode)) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }

        Product product = productMapper.toEntity(request, category);
        product.setCode(productCode);
        product.setBrand(brand);
        product.setStatus(Constants.STATUS_ACTIVE);

        product = productRepository.save(product);
        log.info("Product created: id={}, code={}, brandId={}", product.getId(), product.getCode(), brand.getId());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request) {
        Product product = getProductOrThrow(id);
        String brandId = product.getBrand().getId();
        accessScopeService.assertCanManageBrand(brandId);

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
            validateCategoryOwnership(category, brandId);
            product.setCategory(category);
        }

        if (request.getCode() != null
                && productRepository.existsByBrandIdAndCodeAndIdNot(brandId, request.getCode(), id)) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }

        productMapper.updateEntity(product, request);
        product = productRepository.save(product);

        log.info("Product updated: id={}, code={}", product.getId(), product.getCode());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse updateStatus(String id, UpdateProductStatusRequest request) {
        Product product = getProductOrThrow(id);
        accessScopeService.assertCanManageBrand(product.getBrand().getId());

        product.setStatus(request.getStatus());
        product = productRepository.save(product);

        log.info("Product status updated: id={}, code={}, status={}", product.getId(), product.getCode(), product.getStatus());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(String id) {
        Product product = getProductOrThrow(id);
        accessScopeService.assertCanManageBrand(product.getBrand().getId());

        if (Constants.STATUS_INACTIVE.equals(product.getStatus())) {
            throw new BaseException(ErrorCode.PRODUCT_005);
        }

        product.setStatus(Constants.STATUS_INACTIVE);
        productRepository.save(product);
        log.info("Product deleted (soft): id={}, code={}", product.getId(), product.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getById(String id) {
        Product product = getProductOrThrow(id);
        accessScopeService.assertCanAccessBrand(product.getBrand().getId());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(String brandId, String categoryId, Pageable pageable) {
        Page<Product> products;

        if (brandId != null && !brandId.isBlank()) {
            accessScopeService.assertCanAccessBrand(brandId);
            brandRepository.findById(brandId)
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_003));

            if (categoryId != null && !categoryId.isBlank()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
                validateCategoryOwnership(category, brandId);
                products = productRepository.findByBrandIdAndCategoryId(brandId, categoryId, pageable);
            } else {
                products = productRepository.findByBrandId(brandId, pageable);
            }
        } else if (categoryId != null && !categoryId.isBlank()) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
            accessScopeService.assertCanAccessBrand(category.getBrand().getId());
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            throw new BaseException(ErrorCode.COM_004, "brandId or categoryId is required");
        }

        List<ProductResponse> content = products.getContent().stream()
                .map(productMapper::toResponse)
                .toList();

        return PageResponse.from(products, content);
    }

    private Product getProductOrThrow(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private void validateCategoryOwnership(Category category, String brandId) {
        if (category.getBrand() == null || !brandId.equals(category.getBrand().getId())) {
            throw new BaseException(ErrorCode.PRODUCT_006);
        }
    }

    private String resolveCreateCode(String requestedCode, String brandId) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            return requestedCode.trim();
        }

        String generatedCode = codeGenerator.generate("PR", brandId);
        if (productRepository.existsByBrandIdAndCode(brandId, generatedCode)) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }
        return generatedCode;
    }
}
