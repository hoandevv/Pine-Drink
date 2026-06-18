package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Order.CancelOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.CreateOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.UpdateOrderStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    
    /**
     * Create a new order from customer's cart
     *
     * @param customerId the ID of the customer
     * @param request the order creation request
     * @return the created order response
     */
    OrderResponse createOrder(String customerId, CreateOrderRequest request);
    
    /**
     * Get order by ID (with optional customer ID for ownership check)
     *
     * @param orderId the ID of the order
     * @param customerId the ID of the customer (optional, for ownership check)
     * @return the order response
     */
    OrderResponse getOrderById(String orderId, String customerId);

    default OrderResponse getOrderById(String orderId) {
        return getOrderById(orderId, null);
    }
    
    /**
     * Get order by order code (with optional customer ID for ownership check)
     *
     * @param orderCode the order code
     * @param customerId the ID of the customer (optional, for ownership check)
     * @return the order response
     */
    OrderResponse getOrderByCode(String orderCode, String customerId);

    default OrderResponse getOrderByCode(String orderCode) {
        return getOrderByCode(orderCode, null);
    }
    
    /**
     * Get all orders for a customer
     *
     * @param customerId the ID of the customer
     * @param pageable pagination information
     * @return page of orders
     */
    Page<OrderResponse> getCustomerOrders(String customerId, Pageable pageable);
    
    /**
     * Get all orders for a branch
     *
     * @param branchId the ID of the branch
     * @param status filter by status (optional)
     * @param pageable pagination information
     * @return page of orders
     */
    Page<OrderResponse> getBranchOrders(String branchId, String status, Pageable pageable);
    
    /**
     * Update order status
     *
     * @param orderId the ID of the order
     * @param request the status update request
     * @return the updated order response
     */
    OrderResponse updateOrderStatus(String orderId, UpdateOrderStatusRequest request);
    
    /**
     * Cancel an order
     *
     * @param orderId the ID of the order
     * @param customerId the ID of the customer (for verification)
     * @param request the cancellation request
     * @return the cancelled order response
     */
    OrderResponse cancelOrder(String orderId, String customerId, CancelOrderRequest request);
}
