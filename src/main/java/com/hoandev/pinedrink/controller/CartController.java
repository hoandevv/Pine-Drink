package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.CustomerProfile;
import com.hoandev.pinedrink.entity.dto.request.CartItem.AddCartItemRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.CartItem.CartResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.repository.CustomerProfileRepository;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.CartService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/cart")
@Slf4j
//@PreAuthorize("hasRole('CUSTOMER')")
public class CartController {

    private final CartService cartService;
    private final CustomerProfileRepository customerProfileRepository;

    public CartController(CartService cartService, CustomerProfileRepository customerProfileRepository) {
        this.cartService = cartService;
        this.customerProfileRepository = customerProfileRepository;
    }

    @GetMapping
    public ResponseEntity<BaseResponse<CartResponse>> getActiveCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String branchId) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Getting active cart: customerId={}, branchId={}", customer.getId(), branchId);

        CartResponse response = cartService.getActiveCart(customer.getId(), branchId);
        return ResponseEntity.ok(BaseResponse.success(response, "Cart retrieved successfully"));
    }

    @PostMapping("/items")
    public ResponseEntity<BaseResponse<CartResponse>> addItemToCart(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Adding item to cart: customerId={}, branchId={}, productId={}",
                customer.getId(), request.getBranchId(), request.getProductId());

        CartResponse response = cartService.addItemToCart(customer.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Cart item added successfully"));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<BaseResponse<CartResponse>> removeCartItem(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String itemId) {
        CustomerProfile customer = getCurrentCustomer(principal);
        log.info("Removing cart item: customerId={}, itemId={}", customer.getId(), itemId);

        CartResponse response = cartService.removeCartItem(customer.getId(), itemId);
        return ResponseEntity.ok(BaseResponse.success(response, "Cart item removed successfully"));
    }

    private CustomerProfile getCurrentCustomer(UserPrincipal principal) {
        if (principal == null) {
            throw new BaseException(ErrorCode.AUTH_003);
        }
        return customerProfileRepository.findByAccountId(principal.getId())
                .orElseThrow(() -> new BaseException(ErrorCode.CUSTOMER_001));
    }
}
