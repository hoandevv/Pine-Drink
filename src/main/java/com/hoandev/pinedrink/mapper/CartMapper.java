package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Cart;
import com.hoandev.pinedrink.entity.CartItem;
import com.hoandev.pinedrink.entity.CartItemTopping;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductVariant;
import com.hoandev.pinedrink.entity.Topping;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartItemResponse;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartItemToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart, List<CartItemResponse> items) {
        if (cart == null) {
            return null;
        }

        List<CartItemResponse> safeItems = items != null ? items : new ArrayList<>();

        return CartResponse.builder()
                .id(cart.getId())
                .status(cart.getStatus())
                .branchId(cart.getBranch() != null ? cart.getBranch().getId() : null)
                .branchName(cart.getBranch() != null ? cart.getBranch().getName() : null)
                .customerId(cart.getCustomer() != null ? cart.getCustomer().getId() : null)
                .items(safeItems)
                .totalQuantity(totalQuantity(safeItems))
                .subtotalAmount(subtotalAmount(safeItems))
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    public CartResponse toEmptyResponse(String customerId, String branchId) {
        return CartResponse.builder()
                .customerId(customerId)
                .branchId(branchId)
                .items(new ArrayList<>())
                .totalQuantity(0)
                .subtotalAmount(BigDecimal.ZERO)
                .build();
    }

    public CartItemResponse toItemResponse(CartItem item, List<CartItemToppingResponse> toppings) {
        if (item == null) {
            return null;
        }

        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();
        List<CartItemToppingResponse> safeToppings = toppings != null ? toppings : new ArrayList<>();

        return CartItemResponse.builder()
                .id(item.getId())
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .productImageUrl(product != null ? product.getImageUrl() : null)
                .variantId(variant != null ? variant.getId() : null)
                .variantName(variant != null ? variant.getVariantName() : null)
                .quantity(item.getQuantity())
                .sugarLevel(item.getSugarLevel())
                .iceLevel(item.getIceLevel())
                .note(item.getNote())
                .unitPrice(item.getUnitPrice())
                .toppingAmount(subtotalToppings(safeToppings))
                .totalPrice(item.getTotalPrice())
                .toppings(safeToppings)
                .build();
    }

    public CartItemToppingResponse toToppingResponse(CartItemTopping itemTopping) {
        if (itemTopping == null) {
            return null;
        }

        Topping topping = itemTopping.getTopping();

        return CartItemToppingResponse.builder()
                .id(itemTopping.getId())
                .toppingId(topping != null ? topping.getId() : null)
                .toppingCode(topping != null ? topping.getCode() : null)
                .toppingName(topping != null ? topping.getName() : null)
                .quantity(itemTopping.getQuantity())
                .unitPrice(itemTopping.getUnitPrice())
                .totalPrice(itemTopping.getTotalPrice())
                .build();
    }

    private Integer totalQuantity(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::getQuantity)
                .filter(quantity -> quantity != null)
                .reduce(0, Integer::sum);
    }

    private BigDecimal subtotalAmount(List<CartItemResponse> items) {
        return items.stream()
                .map(CartItemResponse::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal subtotalToppings(List<CartItemToppingResponse> toppings) {
        return toppings.stream()
                .map(CartItemToppingResponse::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
