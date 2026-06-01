package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Category;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.dto.request.Product.CreateProductRequest;
import com.hoandev.pinedrink.entity.dto.request.Product.UpdateProductRequest;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return ProductResponse.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .description(product.getDescription())
                .imageUrl(product.getImageUrl())
                .basePrice(product.getBasePrice())
                .preparationMinutes(product.getPreparationMinutes())
                .featured(product.isFeatured())
                .bestSeller(product.isBestSeller())
                .availableIceLevels(product.getAvailableIceLevels())
                .availableSugarLevels(product.getAvailableSugarLevels())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public Product toEntity(CreateProductRequest request, Category category) {
        if (request == null) {
            return null;
        }

        Product product = new Product();
        product.setCode(request.getCode());
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setImageUrl(request.getImageUrl());
        product.setBasePrice(request.getBasePrice());
        product.setPreparationMinutes(request.getPreparationMinutes() != null ? request.getPreparationMinutes() : 10);
        product.setFeatured(request.isFeatured());
        product.setBestSeller(request.isBestSeller());
        product.setAvailableIceLevels(request.getAvailableIceLevels() != null ? request.getAvailableIceLevels() : "0,30,50,70,100");
        product.setAvailableSugarLevels(request.getAvailableSugarLevels() != null ? request.getAvailableSugarLevels() : "0,30,50,70,100");
        product.setCategory(category);
        return product;
    }

    public void updateEntity(Product product, UpdateProductRequest request) {
        if (product == null || request == null) {
            return;
        }

        if (request.getCode() != null) {
            product.setCode(request.getCode());
        }
        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }
        if (request.getBasePrice() != null) {
            product.setBasePrice(request.getBasePrice());
        }
        if (request.getPreparationMinutes() != null) {
            product.setPreparationMinutes(request.getPreparationMinutes());
        }
        if (request.getFeatured() != null) {
            product.setFeatured(request.getFeatured());
        }
        if (request.getBestSeller() != null) {
            product.setBestSeller(request.getBestSeller());
        }
        if (request.getAvailableIceLevels() != null) {
            product.setAvailableIceLevels(request.getAvailableIceLevels());
        }
        if (request.getAvailableSugarLevels() != null) {
            product.setAvailableSugarLevels(request.getAvailableSugarLevels());
        }
    }
}
