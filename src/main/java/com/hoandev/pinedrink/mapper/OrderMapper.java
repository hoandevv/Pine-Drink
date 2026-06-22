package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.OrderItem;
import com.hoandev.pinedrink.entity.OrderItemTopping;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderItemToppingResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderResponse;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order, List<OrderItemResponse> items) {
        if (order == null) {
            return null;
        }

        List<OrderItemResponse> safeItems = items != null ? items : new ArrayList<>();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .status(order.getStatus())
                .branchId(order.getBranch() != null ? order.getBranch().getId() : null)
                .branchName(order.getBranch() != null ? order.getBranch().getName() : null)
                .branchAddress(order.getBranch() != null ? order.getBranch().getAddress() : null)
                .customerId(order.getCustomer() != null ? order.getCustomer().getId() : null)
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .customerEmail(order.getCustomerEmail())
                .orderType(order.getOrderType())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .pickupTime(order.getPickupTime())
                .deliveryAddress(order.getDeliveryAddress())
                .note(order.getNote())
                .items(safeItems)
                .createdAt(order.getCreatedAt())
                .confirmedAt(order.getConfirmedAt())
                .preparedAt(order.getPreparedAt())
                .readyAt(order.getReadyAt())
                .deliveringAt(order.getDeliveringAt())
                .deliveredAt(order.getDeliveredAt())
                .completedAt(order.getCompletedAt())
                .cancelledAt(order.getCancelledAt())
                .rejectedAt(order.getRejectedAt())
                .cancelReason(order.getCancelReason())
                .build();
    }

    public OrderItemResponse toItemResponse(OrderItem item, List<OrderItemToppingResponse> toppings) {
        if (item == null) {
            return null;
        }

        List<OrderItemToppingResponse> safeToppings = toppings != null ? toppings : new ArrayList<>();

        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .productImageUrl(item.getProduct() != null ? item.getProduct().getImageUrl() : null)
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .sugarLevel(item.getSugarLevel())
                .iceLevel(item.getIceLevel())
                .note(item.getNote())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .toppings(safeToppings)
                .build();
    }

    public OrderItemToppingResponse toToppingResponse(OrderItemTopping itemTopping) {
        if (itemTopping == null) {
            return null;
        }

        return OrderItemToppingResponse.builder()
                .id(itemTopping.getId())
                .toppingId(itemTopping.getTopping() != null ? itemTopping.getTopping().getId() : null)
                .toppingCode(itemTopping.getToppingCode())
                .toppingName(itemTopping.getToppingName())
                .quantity(itemTopping.getQuantity())
                .unitPrice(itemTopping.getUnitPrice())
                .totalPrice(itemTopping.getTotalPrice())
                .build();
    }
}
