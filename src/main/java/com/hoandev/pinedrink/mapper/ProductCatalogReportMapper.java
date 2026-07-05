package com.hoandev.pinedrink.mapper;

import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportDto;
import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportItemDto;
import com.hoandev.pinedrink.repository.result.ProductCatalogResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class ProductCatalogReportMapper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat VND_FORMATTER = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    public ProductCatalogReportDto toReportDto(
            List<ProductCatalogResult> products,
            String statusFilter,
            String categoryFilter
    ) {
        return ProductCatalogReportDto.builder()
                .title("DANH SACH SAN PHAM")
                .generatedAt(LocalDateTime.now().format(DATE_TIME_FORMATTER))
                .statusFilter(defaultText(statusFilter, "ALL"))
                .categoryFilter(defaultText(categoryFilter, "ALL"))
                .totalProducts(String.valueOf(products.size()))
                .items(products.stream()
                        .map(this::toItemDto)
                        .toList())
                .build();
    }

    private ProductCatalogReportItemDto toItemDto(ProductCatalogResult product) {
        return ProductCatalogReportItemDto.builder()
                .productCode(product.getProductCode())
                .productName(product.getProductName())
                .categoryName(product.getCategoryName())
                .basePrice(formatVnd(product.getBasePrice()))
                .status(product.getStatus())
                .preparationMinutes(String.valueOf(product.getPreparationMinutes()))
                .featured(formatBoolean(product.getFeatured()))
                .bestSeller(formatBoolean(product.getBestSeller()))
                .variants(defaultText(product.getVariants(), "-"))
                .createdAt(product.getCreatedAt().format(DATE_TIME_FORMATTER))
                .build();
    }

    private String formatVnd(BigDecimal amount) {
        return VND_FORMATTER.format(amount == null ? BigDecimal.ZERO : amount) + " VND";
    }

    private String formatBoolean(Boolean value) {
        return Boolean.TRUE.equals(value) ? "YES" : "NO";
    }

    private String defaultText(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
