package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.BranchVariantDailyStock;
import com.hoandev.pinedrink.entity.BranchVariantStockLog;
import com.hoandev.pinedrink.entity.Product;
import com.hoandev.pinedrink.entity.ProductVariant;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockLogResponse;
import com.hoandev.pinedrink.entity.dto.response.DailyStock.DailyStockResponse;
import org.springframework.stereotype.Component;

@Component
public class DailyStockMapper {

    public DailyStockResponse toResponse(BranchVariantDailyStock stock) {
        if (stock == null) {
            return null;
        }

        ProductVariant variant = stock.getVariant();
        Product product = variant == null ? null : variant.getProduct();
        int availableQuantity = available(stock);
        return DailyStockResponse.builder()
                .id(stock.getId())
                .branchId(stock.getBranch().getId())
                .branchName(stock.getBranch().getName())
                .productId(product == null ? null : product.getId())
                .productName(product == null ? null : product.getName())
                .variantId(variant == null ? null : variant.getId())
                .variantName(variant == null ? null : variant.getVariantName())
                .stockDate(stock.getStockDate())
                .dailyQuantity(stock.getDailyQuantity())
                .soldQuantity(stock.getSoldQuantity())
                .reservedQuantity(stock.getReservedQuantity())
                .availableQuantity(availableQuantity)
                .stockStatus(availableQuantity > 0 ? "AVAILABLE" : "OUT_OF_STOCK")
                .status(stock.getStatus())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }

    public DailyStockLogResponse toLogResponse(BranchVariantStockLog log) {
        if (log == null) {
            return null;
        }

        return DailyStockLogResponse.builder()
                .id(log.getId())
                .dailyStockId(log.getDailyStock().getId())
                .orderId(log.getOrder() == null ? null : log.getOrder().getId())
                .actionType(log.getActionType())
                .quantity(log.getQuantity())
                .beforeDailyQuantity(log.getBeforeDailyQuantity())
                .afterDailyQuantity(log.getAfterDailyQuantity())
                .beforeSoldQuantity(log.getBeforeSoldQuantity())
                .afterSoldQuantity(log.getAfterSoldQuantity())
                .beforeReservedQuantity(log.getBeforeReservedQuantity())
                .afterReservedQuantity(log.getAfterReservedQuantity())
                .reason(log.getReason())
                .createdBy(log.getCreatedBy())
                .createdAt(log.getCreatedAt())
                .build();
    }

    private int available(BranchVariantDailyStock stock) {
        return stock.getDailyQuantity() - stock.getSoldQuantity() - stock.getReservedQuantity();
    }
}
