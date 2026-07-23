package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.request.Order.CancelOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.CreateOrderRequest;
import com.hoandev.pinedrink.entity.dto.request.Order.UpdateOrderStatusRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderListItemResponse;
import com.hoandev.pinedrink.entity.dto.response.Order.OrderResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CustomerProfileRepository customerProfileRepository;

    /**
     * Create a new order from cart
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PERM_ORDER_CREATE')")
    public ResponseEntity<BaseResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Creating order: customerId={}, branchId={}", customer.getId(), request.getBranchId());

        OrderResponse response = orderService.createOrder(customer.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Order created successfully"));
    }

    /**
     * Get all orders (for admin dashboards)
     */
    @GetMapping
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW')")
    public ResponseEntity<BaseResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("Getting all orders: status={}", status);

        Page<OrderResponse> response = orderService.getAllOrders(status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Orders retrieved successfully"));
    }

    /**
     * Get order by ID
     */
    @GetMapping("/summaries")
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW')")
    public ResponseEntity<BaseResponse<Page<OrderListItemResponse>>> getAllOrderSummaries(
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("Getting all order summaries: status={}", status);
        Page<OrderListItemResponse> response = orderService.getAllOrderSummaries(status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Order summaries retrieved successfully"));
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyAuthority('PERM_ORDER_VIEW', 'PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId) {
        log.info("Getting order by ID: orderId={}", orderId);

        String customerId = shouldRestrictToOwnOrders(principal) ? getCurrentCustomer(principal).getId() : null;
        OrderResponse response = orderService.getOrderById(orderId, customerId);
        return ResponseEntity.ok(BaseResponse.success(response, "Order retrieved successfully"));
    }

    /**
     * Get order by code
     */
    @GetMapping("/code/{orderCode}")
    @PreAuthorize("hasAnyAuthority('PERM_ORDER_VIEW', 'PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<OrderResponse>> getOrderByCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderCode) {
        log.info("Getting order by code: orderCode={}", orderCode);

        String customerId = shouldRestrictToOwnOrders(principal) ? getCurrentCustomer(principal).getId() : null;
        OrderResponse response = orderService.getOrderByCode(orderCode, customerId);
        return ResponseEntity.ok(BaseResponse.success(response, "Order retrieved successfully"));
    }

    /**
     * Get customer's orders
     */
    @GetMapping("/my-orders")
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Getting customer orders: customerId={}", customer.getId());

        Page<OrderResponse> response = orderService.getCustomerOrders(customer.getId(), pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Orders retrieved successfully"));
    }

    /**
     * Get branch orders (for staff/manager)
     */
    @GetMapping("/my-orders/summaries")
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<Page<OrderListItemResponse>>> getMyOrderSummaries(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Getting customer order summaries: customerId={}", customer.getId());
        Page<OrderListItemResponse> response = orderService.getCustomerOrderSummaries(customer.getId(), pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Order summaries retrieved successfully"));
    }

    @GetMapping("/branch/{branchId}/summaries")
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW_BRANCH')")
    public ResponseEntity<BaseResponse<Page<OrderListItemResponse>>> getBranchOrderSummaries(
            @PathVariable String branchId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("Getting branch order summaries: branchId={}, status={}", branchId, status);
        Page<OrderListItemResponse> response = orderService.getBranchOrderSummaries(branchId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Order summaries retrieved successfully"));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAuthority('PERM_ORDER_VIEW_BRANCH')")
    public ResponseEntity<BaseResponse<Page<OrderResponse>>> getBranchOrders(
            @PathVariable String branchId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        log.info("Getting branch orders: branchId={}, status={}", branchId, status);

        Page<OrderResponse> response = orderService.getBranchOrders(branchId, status, pageable);
        return ResponseEntity.ok(BaseResponse.success(response, "Orders retrieved successfully"));
    }

    /**
     * Update order status (for staff/manager)
     */
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAuthority('PERM_ORDER_UPDATE_STATUS')")
    public ResponseEntity<BaseResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        log.info("Updating order status: orderId={}, status={}", orderId, request.getStatus());

        OrderResponse response = orderService.updateOrderStatus(orderId, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Order status updated successfully"));
    }

    /**
     * Cancel order (for customer)
     */
    @PostMapping("/{orderId}/cancel")
    @PreAuthorize("hasAuthority('PERM_ORDER_CANCEL')")
    public ResponseEntity<BaseResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String orderId,
            @Valid @RequestBody CancelOrderRequest request) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Cancelling order: orderId={}, customerId={}", orderId, customer.getId());

        OrderResponse response = orderService.cancelOrder(orderId, customer.getId(), request);
        return ResponseEntity.ok(BaseResponse.success(response, "Order cancelled successfully"));
    }

    private boolean shouldRestrictToOwnOrders(UserPrincipal principal) {
        if (principal == null) {
            return true;
        }
        boolean canViewAllOrders = principal.getPermissionAuthorities().contains("PERM_ORDER_VIEW");
        return !canViewAllOrders;
    }

    private CustomerProfile getCurrentCustomer(UserPrincipal principal) {
        if (principal == null) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        return customerProfileRepository.findByAccountId(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_001));
    }
}
