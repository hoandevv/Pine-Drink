package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(String id, UpdateProductRequest request);

    ProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    void delete(String id);

    ProductResponse getById(String id);

    PageResponse<ProductResponse> getAll(String brandId, String categoryId, Pageable pageable);
}
