package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Payment.CreateRefundRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoCreatePaymentRequest;
import com.hoandev.pinedrink.entity.dto.request.Payment.MomoIpnRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoCreatePaymentResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.MomoIpnResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;
import com.hoandev.pinedrink.entity.dto.response.Payment.RefundResponse;
import com.hoandev.pinedrink.service.PaymentService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/offline/record")
    @PreAuthorize("hasAuthority('PERM_ORDER_UPDATE_STATUS')")
    public ResponseEntity<BaseResponse<PaymentTransactionResponse>> recordOfflinePayment(
            @Valid @RequestBody RecordOfflinePaymentRequest request) {
        log.info("Recording offline payment: orderId={}, method={}", request.getOrderId(), request.getPaymentMethod());
        PaymentTransactionResponse response = paymentService.recordOfflinePayment(request);
        return ResponseEntity.ok(BaseResponse.success(response, "Offline payment recorded successfully"));
    }

    @GetMapping("/orders/{orderId}/status")
    @PreAuthorize("hasAnyAuthority('PERM_ORDER_VIEW', 'PERM_ORDER_VIEW_BRANCH', 'PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<PaymentTransactionResponse>> getOrderPaymentStatus(
            @PathVariable String orderId) {
        log.info("Getting payment status: orderId={}", orderId);
        PaymentTransactionResponse response = paymentService.getLatestOrderPaymentStatus(orderId);
        return ResponseEntity.ok(BaseResponse.success(response, "Payment status retrieved successfully"));
    }

    @PostMapping("/refunds")
    @PreAuthorize("hasAuthority('PERM_ORDER_UPDATE_STATUS')")
    public ResponseEntity<BaseResponse<RefundResponse>> createRefund(
            @Valid @RequestBody CreateRefundRequest request) {
        RefundResponse response = paymentService.createRefund(request);
        return ResponseEntity.ok(BaseResponse.success(response, "Refund created successfully"));
    }

    @PostMapping("/momo/create")
    @PreAuthorize("hasAnyAuthority('PERM_ORDER_VIEW', 'PERM_ORDER_VIEW_BRANCH', 'PERM_ORDER_VIEW_OWN')")
    public ResponseEntity<BaseResponse<MomoCreatePaymentResponse>> createMomoPayment(
            @Valid @RequestBody MomoCreatePaymentRequest request) {
        MomoCreatePaymentResponse response = paymentService.createMomoPayment(request);
        return ResponseEntity.ok(BaseResponse.success(response, "MoMo payment created successfully"));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<MomoIpnResponse> handleMomoIpn(@RequestBody MomoIpnRequest request) {
        log.info("Received MoMo IPN: orderId={}, requestId={}, resultCode={}, amount={}",
                request.getOrderId(), request.getRequestId(), request.getResultCode(), request.getAmount());
        return ResponseEntity.ok(paymentService.handleMomoIpn(request));
    }

    @GetMapping("/momo/return")
    public ResponseEntity<BaseResponse<MomoIpnResponse>> handleMomoReturn(@RequestParam Map<String, String> params) {
        MomoIpnResponse response = paymentService.handleMomoReturn(params);
        return ResponseEntity.ok(BaseResponse.success(response, "MoMo return verified"));
    }
}
