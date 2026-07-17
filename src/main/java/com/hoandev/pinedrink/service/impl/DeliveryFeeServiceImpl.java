package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.CustomerAddress;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class DeliveryFeeServiceImpl implements DeliveryFeeService {

    private static final BigDecimal EARTH_RADIUS_KM = BigDecimal.valueOf(6371);

    private final OrderProperties orderProperties;

    @Override
    public BigDecimal calculate(Branch branch, CustomerAddress address, BigDecimal subtotal) {
        OrderProperties.Delivery delivery = orderProperties.getDelivery();
        if (subtotal.compareTo(delivery.getFreeThreshold()) >= 0) {
            return BigDecimal.ZERO;
        }
        validateCoordinates(branch, address);

        BigDecimal distanceKm = haversineKm(
                branch.getLatitude(), branch.getLongitude(),
                address.getLatitude(), address.getLongitude()
        ).multiply(delivery.getRoadFactor());

        if (delivery.getMaxDistanceKm() != null && distanceKm.compareTo(delivery.getMaxDistanceKm()) > 0) {
            throw new BaseException(ErrorCode.COM_004);
        }

        return delivery.getBaseFee()
                .add(distanceKm.multiply(delivery.getFeePerKm()))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private void validateCoordinates(Branch branch, CustomerAddress address) {
        if (branch.getLatitude() == null || branch.getLongitude() == null) {
            throw new BaseException(ErrorCode.BRANCH_001);
        }
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            throw new BaseException(ErrorCode.CUSTOMER_002);
        }
    }

    private BigDecimal haversineKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double latDistance = Math.toRadians(lat2.subtract(lat1).doubleValue());
        double lonDistance = Math.toRadians(lon2.subtract(lon1).doubleValue());
        double startLat = Math.toRadians(lat1.doubleValue());
        double endLat = Math.toRadians(lat2.doubleValue());

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(startLat) * Math.cos(endLat)
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM.multiply(BigDecimal.valueOf(c));
    }
}
