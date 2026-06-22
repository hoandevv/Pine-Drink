package com.hoandev.pinedrink.service;

public interface OrderExpiryService {

    /**
     * Expire a PENDING order that was not confirmed within the allowed time.
     * Idempotent: does nothing if order is not found or already processed.
     *
     * @param orderId the order ID to expire
     */
    void expire(String orderId);
}
