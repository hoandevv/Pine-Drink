package com.hoandev.pinedrink.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "order")
@Getter
@Setter
public class OrderProperties {
    
    private Delivery delivery = new Delivery();
    private Cancel cancel = new Cancel();
    
    @Getter
    @Setter
    public static class Delivery {
        /**
         * Default delivery fee in VND
         */
        private BigDecimal defaultFee = BigDecimal.valueOf(15000);

        private BigDecimal baseFee = BigDecimal.valueOf(10000);

        private BigDecimal feePerKm = BigDecimal.valueOf(5000);

        private BigDecimal maxDistanceKm = BigDecimal.valueOf(15);

        private BigDecimal roadFactor = BigDecimal.valueOf(1.25);
        
        /**
         * Free delivery threshold in VND
         */
        private BigDecimal freeThreshold = BigDecimal.valueOf(200000);

        public BigDecimal getDefaultFee() {
            return defaultFee;
        }

        public void setDefaultFee(BigDecimal defaultFee) {
            this.defaultFee = defaultFee;
        }

        public BigDecimal getBaseFee() {
            return baseFee;
        }

        public void setBaseFee(BigDecimal baseFee) {
            this.baseFee = baseFee;
        }

        public BigDecimal getFeePerKm() {
            return feePerKm;
        }

        public void setFeePerKm(BigDecimal feePerKm) {
            this.feePerKm = feePerKm;
        }

        public BigDecimal getMaxDistanceKm() {
            return maxDistanceKm;
        }

        public void setMaxDistanceKm(BigDecimal maxDistanceKm) {
            this.maxDistanceKm = maxDistanceKm;
        }

        public BigDecimal getRoadFactor() {
            return roadFactor;
        }

        public void setRoadFactor(BigDecimal roadFactor) {
            this.roadFactor = roadFactor;
        }

        public BigDecimal getFreeThreshold() {
            return freeThreshold;
        }

        public void setFreeThreshold(BigDecimal freeThreshold) {
            this.freeThreshold = freeThreshold;
        }
    }
    
    @Getter
    @Setter
    public static class Cancel {
        /**
         * Allowed statuses for order cancellation
         */
        private List<String> allowedStatuses = List.of("PENDING", "CONFIRMED");
        
        /**
         * Timeout in minutes for order cancellation
         */
        private Integer timeoutMinutes = 30;
    }
}
