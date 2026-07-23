package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.AssignProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingSummaryResponse;

import java.util.List;

public interface ProductToppingService {
    ProductToppingResponse assign(String productId, AssignProductToppingRequest request);

    ProductToppingResponse update(String productId, String productToppingId, UpdateProductToppingRequest request);

    ProductToppingResponse updateStatus(String productId, String productToppingId, UpdateProductToppingStatusRequest request);

    void delete(String productId, String productToppingId);

    ProductToppingResponse getById(String productId, String productToppingId);

    List<ProductToppingSummaryResponse> getAll(String productId);

    List<ProductToppingSummaryResponse> getAllActive(String productId);
}
