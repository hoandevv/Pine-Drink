package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.CustomerAddress;

import java.math.BigDecimal;

public interface DeliveryFeeService {
    /**
     * Calculates the delivery fee based on the branch, customer address, and subtotal.
     *
     * @param branch the branch for which to calculate the delivery fee
     * @param address the customer's address
     * @param subtotal the subtotal amount
     * @return the calculated delivery fee
     */
    BigDecimal calculate(Branch branch, CustomerAddress address, BigDecimal subtotal);
}
