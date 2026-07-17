package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ProductService {
    ProductResponse create(CreateProductRequest request);

    ProductResponse create(CreateProductRequest request, MultipartFile imageFile);

    ProductResponse update(String id, UpdateProductRequest request);

    ProductResponse update(String id, UpdateProductRequest request, MultipartFile imageFile);

    ProductResponse updateStatus(String id, UpdateProductStatusRequest request);

    void delete(String id);

    ProductResponse getById(String id);

    PageResponse<ProductSummaryResponse> getSummaries(String keyword, String categoryId, String status, Pageable pageable);

    PageResponse<ProductResponse> getAll(String keyword, String categoryId, String status, Pageable pageable);
}
