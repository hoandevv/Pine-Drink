package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.ProductVariant.CreateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductVariant.UpdateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductVariant.UpdateProductVariantStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.ProductVariant.ProductVariantResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductVariantService {
    ProductVariantResponse create(String productId, CreateProductVariantRequest request);

    ProductVariantResponse update(String productId, String variantId, UpdateProductVariantRequest request);

    ProductVariantResponse updateStatus(String productId, String variantId, UpdateProductVariantStatusRequest request);

    void delete(String productId, String variantId);

    ProductVariantResponse getById(String productId, String variantId);

    PageResponse<ProductVariantResponse> getAll(String productId, Pageable pageable);

    List<ProductVariantResponse> getAllActive(String productId);
}
