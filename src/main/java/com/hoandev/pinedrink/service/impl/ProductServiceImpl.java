package com.hoandev.pinedrink.service.impl;

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
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        accessScopeService.assertSystemAccess();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
        String productCode = resolveCreateCode(request.getCode());
        Product product = productMapper.toEntity(request, category);
        product.setCode(productCode);
        product.setStatus(Constants.STATUS_ACTIVE);
        product = productRepository.save(product);
        log.info("Product created: id={}, code={}", product.getId(), product.getCode());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse update(String id, UpdateProductRequest request) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
            product.setCategory(category);
        }
        if (request.getCode() != null && productRepository.existsByCodeAndIdNot(request.getCode(), id)) {
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
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
        product.setStatus(request.getStatus());
        product = productRepository.save(product);
        log.info("Product status updated: id={}, code={}, status={}", product.getId(), product.getCode(), product.getStatus());
        return productMapper.toResponse(product);
    }

    @Override
    @Transactional
    public void delete(String id) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(id);
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
        return productMapper.toResponse(getProductOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> getAll(String categoryId, Pageable pageable) {
        Page<Product> products;
        if (categoryId != null && !categoryId.isBlank()) {
            categoryRepository.findById(categoryId).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_004));
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        List<ProductResponse> content = products.getContent().stream().map(productMapper::toResponse).toList();
        return PageResponse.from(products, content);
    }

    private Product getProductOrThrow(String id) {
        return productRepository.findById(id).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private String resolveCreateCode(String requestedCode) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String code = requestedCode.trim();
            if (productRepository.existsByCode(code)) {
                throw new BaseException(ErrorCode.PRODUCT_002);
            }
            return code;
        }
        String generatedCode = codeGenerator.generate("PR", "PRODUCT");
        if (productRepository.existsByCode(generatedCode)) {
            throw new BaseException(ErrorCode.PRODUCT_002);
        }
        return generatedCode;
    }
}
