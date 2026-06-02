package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.request.Topping.CreateToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.Topping.UpdateToppingRequest;
import com.hoandev.pinedrink.entity.dto.response.Topping.ToppingResponse;
import org.springframework.stereotype.Component;

@Component
public class ToppingMapper {

    public ToppingResponse toResponse(Topping topping) {
        if (topping == null) {
            return null;
        }

        return ToppingResponse.builder()
                .id(topping.getId())
                .code(topping.getCode())
                .name(topping.getName())
                .price(topping.getPrice())
                .imageUrl(topping.getImageUrl())
                .groupName(topping.getGroupName())
                .status(topping.getStatus())
                .createdAt(topping.getCreatedAt())
                .updatedAt(topping.getUpdatedAt())
                .build();
    }

    public Topping toEntity(CreateToppingRequest request) {
        if (request == null) {
            return null;
        }

        Topping topping = new Topping();
        topping.setName(request.getName());
        topping.setPrice(request.getPrice());
        topping.setImageUrl(request.getImageUrl());
        topping.setGroupName(request.getGroupName());
        return topping;
    }

    public void updateEntity(Topping topping, UpdateToppingRequest request) {
        if (topping == null || request == null) {
            return;
        }

        if (request.getName() != null) {
            topping.setName(request.getName());
        }
        if (request.getPrice() != null) {
            topping.setPrice(request.getPrice());
        }
        if (request.getImageUrl() != null) {
            topping.setImageUrl(request.getImageUrl());
        }
        if (request.getGroupName() != null) {
            topping.setGroupName(request.getGroupName());
        }
    }
}
