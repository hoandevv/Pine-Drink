package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductTopping;
import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.AssignProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.request.ProductTopping.UpdateProductToppingRequest;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Product.ProductToppingSummaryResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductToppingMapper {

    public ProductToppingResponse toResponse(ProductTopping productTopping) {
        if (productTopping == null) {
            return null;
        }

        Product product = productTopping.getProduct();
        Topping topping = productTopping.getTopping();

        return ProductToppingResponse.builder()
                .id(productTopping.getId())
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .toppingId(topping != null ? topping.getId() : null)
                .toppingCode(topping != null ? topping.getCode() : null)
                .toppingName(topping != null ? topping.getName() : null)
                .toppingPrice(topping != null ? topping.getPrice() : null)
                .toppingImageUrl(topping != null ? topping.getImageUrl() : null)
                .toppingGroupName(topping != null ? topping.getGroupName() : null)
                .isDefault(productTopping.isDefault())
                .maxQuantity(productTopping.getMaxQuantity())
                .status(productTopping.getStatus())
                .createdAt(productTopping.getCreatedAt())
                .updatedAt(productTopping.getUpdatedAt())
                .build();
    }

    public ProductToppingSummaryResponse toSummaryResponse(ProductTopping productTopping) {
        if (productTopping == null) {
            return null;
        }

        Product product = productTopping.getProduct();
        Topping topping = productTopping.getTopping();

        return ProductToppingSummaryResponse.builder()
                .id(productTopping.getId())
                .productId(product != null ? product.getId() : null)
                .toppingId(topping != null ? topping.getId() : null)
                .toppingCode(topping != null ? topping.getCode() : null)
                .toppingName(topping != null ? topping.getName() : null)
                .toppingPrice(topping != null ? topping.getPrice() : null)
                .toppingImageUrl(topping != null ? topping.getImageUrl() : null)
                .toppingGroupName(topping != null ? topping.getGroupName() : null)
                .isDefault(productTopping.isDefault())
                .maxQuantity(productTopping.getMaxQuantity())
                .status(productTopping.getStatus())
                .build();
    }

    public ProductTopping toEntity(AssignProductToppingRequest request, Product product, Topping topping) {
        if (request == null) {
            return null;
        }

        ProductTopping productTopping = new ProductTopping();
        productTopping.setProduct(product);
        productTopping.setTopping(topping);
        productTopping.setDefault(request.isDefault());
        productTopping.setMaxQuantity(request.getMaxQuantity() != null ? request.getMaxQuantity() : 3);
        return productTopping;
    }

    public void updateEntity(ProductTopping productTopping, UpdateProductToppingRequest request) {
        if (productTopping == null || request == null) {
            return;
        }

        if (request.getIsDefault() != null) {
            productTopping.setDefault(request.getIsDefault());
        }
        if (request.getMaxQuantity() != null) {
            productTopping.setMaxQuantity(request.getMaxQuantity());
        }
    }
}
