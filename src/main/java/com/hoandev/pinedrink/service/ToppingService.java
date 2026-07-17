package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.ProductTopping.CreateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateToppingStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ToppingSummaryResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ToppingService {
    ToppingResponse create(CreateToppingRequest request);

    ToppingResponse create(CreateToppingRequest request, MultipartFile imageFile);

    ToppingResponse update(String id, UpdateToppingRequest request);

    ToppingResponse update(String id, UpdateToppingRequest request, MultipartFile imageFile);

    ToppingResponse updateStatus(String id, UpdateToppingStatusRequest request);

    void delete(String id);

    ToppingResponse getById(String id);

    PageResponse<ToppingSummaryResponse> getAll(Pageable pageable);

    List<ToppingSummaryResponse> getAllActive();
}
