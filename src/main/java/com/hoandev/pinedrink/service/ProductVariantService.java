package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.CreateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductVariantStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductVariantSummaryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductVariantService {
    ProductVariantResponse create(String productId, CreateProductVariantRequest request);

    ProductVariantResponse update(String productId, String variantId, UpdateProductVariantRequest request);

    ProductVariantResponse updateStatus(String productId, String variantId, UpdateProductVariantStatusRequest request);

    void delete(String productId, String variantId);

    ProductVariantResponse getById(String productId, String variantId);

    PageResponse<ProductVariantSummaryResponse> getAll(String productId, Pageable pageable);

    List<ProductVariantSummaryResponse> getAllActive(String productId);

    List<ProductVariantSummaryResponse> getAllActiveForProducts();
}
