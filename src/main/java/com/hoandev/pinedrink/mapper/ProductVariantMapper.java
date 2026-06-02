package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductVariant;
import com.hoandev.pinedrink.entity.dto.request.ProductVariant.CreateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductVariant.UpdateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.response.ProductVariant.ProductVariantResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductVariantMapper {

    public ProductVariantResponse toResponse(ProductVariant variant) {
        if (variant == null) {
            return null;
        }

        Product product = variant.getProduct();
        BigDecimal basePrice = product != null && product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO;
        BigDecimal priceDelta = variant.getPriceDelta() != null ? variant.getPriceDelta() : BigDecimal.ZERO;

        return ProductVariantResponse.builder()
                .id(variant.getId())
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .variantCode(variant.getVariantCode())
                .variantName(variant.getVariantName())
                .sizeLabel(variant.getSizeLabel())
                .priceDelta(priceDelta)
                .finalPrice(basePrice.add(priceDelta))
                .displayOrder(variant.getDisplayOrder())
                .status(variant.getStatus())
                .createdAt(variant.getCreatedAt())
                .updatedAt(variant.getUpdatedAt())
                .build();
    }

    public ProductVariant toEntity(CreateProductVariantRequest request, Product product) {
        if (request == null) {
            return null;
        }

        ProductVariant variant = new ProductVariant();
        variant.setVariantName(request.getVariantName());
        variant.setSizeLabel(request.getSizeLabel());
        variant.setPriceDelta(request.getPriceDelta() != null ? request.getPriceDelta() : BigDecimal.ZERO);
        variant.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        variant.setProduct(product);
        return variant;
    }

    public void updateEntity(ProductVariant variant, UpdateProductVariantRequest request) {
        if (variant == null || request == null) {
            return;
        }

        if (request.getVariantName() != null) {
            variant.setVariantName(request.getVariantName());
        }
        if (request.getSizeLabel() != null) {
            variant.setSizeLabel(request.getSizeLabel());
        }
        if (request.getPriceDelta() != null) {
            variant.setPriceDelta(request.getPriceDelta());
        }
        if (request.getDisplayOrder() != null) {
            variant.setDisplayOrder(request.getDisplayOrder());
        }
    }
}
