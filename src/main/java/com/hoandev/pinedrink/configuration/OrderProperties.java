package com.hoandev.pinedrink.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cấu hình liên quan đến đơn hàng (order).
 * <p>
 * Các giá trị này được bind từ cấu hình (prefix = "order") và dùng để
 * tính phí giao hàng, timeout huỷ đơn, và timeout tự huỷ.
 */
@Configuration
@ConfigurationProperties(prefix = "order")
@Getter
@Setter
public class OrderProperties {
    
    /** Cấu hình phí giao hàng */
    private Delivery delivery = new Delivery();

    /** Cấu hình huỷ đơn */
    private Cancel cancel = new Cancel();

    /** Cấu hình timeout tự huỷ/expire */
    private Expire expire = new Expire();
    
    @Getter
    @Setter
    public static class Delivery {
        /**
         * Phí giao hàng mặc định (VND) nếu không tính theo khoảng cách.
         */
        private BigDecimal defaultFee = BigDecimal.valueOf(15000);

        /**
         * Phí cơ bản (VND) khi bắt đầu tính phí theo khoảng cách.
         */
        private BigDecimal baseFee = BigDecimal.valueOf(10000);

        /**
         * Phí mỗi km (VND).
         */
        private BigDecimal feePerKm = BigDecimal.valueOf(5000);

        /**
         * Khoảng cách tối đa (km) để áp dụng tính phí.
         */
        private BigDecimal maxDistanceKm = BigDecimal.valueOf(15);

        /**
         * Hệ số đường (để nhân với khoảng cách thực tế nếu cần).
         */
        private BigDecimal roadFactor = BigDecimal.valueOf(1.25);
        
        /**
         * Ngưỡng miễn phí giao hàng (VND) nếu tổng đơn >= giá trị này.
         */
        private BigDecimal freeThreshold = BigDecimal.valueOf(200000);
    }
    
    @Getter
    @Setter
    public static class Cancel {
        /**
         * Các trạng thái được phép huỷ đơn (ví dụ: PENDING, CONFIRMED).
         */
        private List<String> allowedStatuses = List.of("PENDING", "CONFIRMED");
        
        /**
         * Thời hạn (phút) cho phép huỷ đơn trước khi không còn được huỷ.
         */
        private Integer timeoutMinutes = 30;
    }

    @Getter
    @Setter
    public static class Expire {
        /**
         * Thời hạn (phút) để tự động từ chối các đơn ở trạng thái PENDING.
         */
        private Integer timeoutMinutes = 15;
    }
}
