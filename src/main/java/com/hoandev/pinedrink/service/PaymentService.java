package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Payment.RecordOfflinePaymentRequest;
import com.hoandev.pinedrink.entity.dto.response.Payment.PaymentTransactionResponse;

public interface PaymentService {
    PaymentTransactionResponse recordOfflinePayment(RecordOfflinePaymentRequest request);

    PaymentTransactionResponse getLatestOrderPaymentStatus(String orderId);
}
