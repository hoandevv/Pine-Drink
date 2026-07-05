package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductVariant;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.CreateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantResponse;
import com.hoandev.pinedrink.entity.enums.ProductStatus;
import com.hoandev.pinedrink.entity.enums.ProductVariantStatus;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ProductVariantMapper;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.repository.ProductVariantRepository;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ProductVariantService;
import com.hoandev.pinedrink.utils.CodeGenerator;
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
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper productVariantMapper;
    private final AccessScopeService accessScopeService;
    private final CodeGenerator codeGenerator;

    @Override
    @Transactional
    public ProductVariantResponse create(String productId, CreateProductVariantRequest request) {
        accessScopeService.assertSystemAccess();
        Product product = getProductOrThrow(productId);
        String variantCode = resolveCreateCode(productId);
        ProductVariant variant = productVariantMapper.toEntity(request, product);
        variant.setVariantCode(variantCode);
        variant.setStatus(ProductVariantStatus.ACTIVE.getValue());
        variant = productVariantRepository.save(variant);
        log.info("Product variant created: id={}, productId={}, variantCode={}", variant.getId(), productId, variant.getVariantCode());
        return productVariantMapper.toResponse(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse update(String productId, String variantId, UpdateProductVariantRequest request) {
        accessScopeService.assertSystemAccess();
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        productVariantMapper.updateEntity(variant, request);
        variant = productVariantRepository.save(variant);
        log.info("Product variant updated: id={}, productId={}, variantCode={}", variant.getId(), productId, variant.getVariantCode());
        return productVariantMapper.toResponse(variant);
    }

    @Override
    @Transactional
    public ProductVariantResponse updateStatus(String productId, String variantId, UpdateProductVariantStatusRequest request) {
        accessScopeService.assertSystemAccess();
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        variant.setStatus(request.getStatus().getValue());
        variant = productVariantRepository.save(variant);
        log.info("Product variant status updated: id={}, productId={}, variantCode={}, status={}", variant.getId(), productId, variant.getVariantCode(), variant.getStatus());
        return productVariantMapper.toResponse(variant);
    }

    @Override
    @Transactional
    public void delete(String productId, String variantId) {
        accessScopeService.assertSystemAccess();
        ProductVariant variant = getVariantOrThrow(productId, variantId);
        if (ProductVariantStatus.INACTIVE.getValue().equals(variant.getStatus())) {
            throw new BaseException(ErrorCode.PRODUCT_009);
        }
        variant.setStatus(ProductVariantStatus.INACTIVE.getValue());
        productVariantRepository.save(variant);
        log.info("Product variant deleted (soft): id={}, productId={}, variantCode={}", variant.getId(), productId, variant.getVariantCode());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getById(String productId, String variantId) {
        return productVariantMapper.toResponse(getVariantOrThrow(productId, variantId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductVariantResponse> getAll(String productId, Pageable pageable) {
        getProductOrThrow(productId);
        Page<ProductVariant> variants = productVariantRepository.findByProductId(productId, pageable);
        List<ProductVariantResponse> content = variants.getContent().stream().map(productVariantMapper::toResponse).toList();
        return PageResponse.from(variants, content);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAllActive(String productId) {
        Product product = getProductOrThrow(productId);
        if (!ProductStatus.ACTIVE.getValue().equals(product.getStatus()) && !ProductStatus.OUT_OF_STOCK.getValue().equals(product.getStatus())) {
            return List.of();
        }
        return productVariantRepository.findByProductIdAndStatusOrderByDisplayOrder(productId, ProductVariantStatus.ACTIVE.getValue())
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> getAllActiveForProducts() {
        return productVariantRepository.findActiveVariantsForProducts(
                        ProductVariantStatus.ACTIVE.getValue(),
                        List.of(ProductStatus.ACTIVE.getValue(), ProductStatus.OUT_OF_STOCK.getValue()))
                .stream()
                .map(productVariantMapper::toResponse)
                .toList();
    }

    private Product getProductOrThrow(String productId) {
        return productRepository.findById(productId).orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));
    }

    private ProductVariant getVariantOrThrow(String productId, String variantId) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_007));
        if (variant.getProduct() == null || !productId.equals(variant.getProduct().getId())) {
            throw new BaseException(ErrorCode.PRODUCT_010);
        }
        return variant;
    }

    private String resolveCreateCode(String productId) {
        String generatedCode = codeGenerator.generate("PV", "PRODUCT_VARIANT");
        if (productVariantRepository.existsByProductIdAndVariantCode(productId, generatedCode)) {
            throw new BaseException(ErrorCode.PRODUCT_008);
        }
        return generatedCode;
    }
}
