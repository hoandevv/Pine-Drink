package com.hoandev.pinedrink.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoandev.pinedrink.entity.ExportRequest;
import com.hoandev.pinedrink.entity.dto.report.DailyRevenueReportDto;
import com.hoandev.pinedrink.entity.dto.report.InvoiceReportDto;
import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportDto;
import com.hoandev.pinedrink.entity.enums.ExportRequestStatus;
import com.hoandev.pinedrink.entity.enums.ReportType;
import com.hoandev.pinedrink.mapper.DailyRevenueReportMapper;
import com.hoandev.pinedrink.mapper.InvoiceReportMapper;
import com.hoandev.pinedrink.mapper.ProductCatalogReportMapper;
import com.hoandev.pinedrink.repository.ExportRequestRepository;
import com.hoandev.pinedrink.repository.OrderItemRepository;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
import com.hoandev.pinedrink.repository.result.DailyRevenuePaymentResult;
import com.hoandev.pinedrink.repository.result.DailyRevenueSummaryResult;
import com.hoandev.pinedrink.repository.result.InvoiceHeaderResult;
import com.hoandev.pinedrink.repository.result.InvoiceItemResult;
import com.hoandev.pinedrink.repository.result.ProductCatalogResult;
import com.hoandev.pinedrink.service.JasperReportService;
import com.hoandev.pinedrink.service.ReportExportService;
import com.hoandev.pinedrink.service.ReportStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation mặc định của {@link ReportExportService}.
 * <p>
 * Service này thực hiện luồng xuất báo cáo thực tế sau khi bộ lắng nghe
 * RabbitMQ
 * nhận được thông điệp job. Việc giữ logic này ở tầng service giúp luồng xử lý
 * có thể
 * tái sử dụng và giữ package queue chỉ tập trung vào trách nhiệm vận chuyển
 * thông điệp.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportServiceImpl implements ReportExportService {

    private final ExportRequestRepository exportRequestRepository;
    private final JasperReportService jasperReportService;
    private final ReportStorageService reportStorageService;
    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final InvoiceReportMapper invoiceReportMapper;
    private final DailyRevenueReportMapper dailyRevenueReportMapper;
    private final ProductCatalogReportMapper productCatalogReportMapper;

    /**
     * Thực thi một job xuất báo cáo đang chờ xử lý và lưu trạng thái cuối cùng.
     * <p>
     * Method này bỏ qua job không tồn tại hoặc job không còn ở trạng thái chờ xử lý
     * để thông điệp RabbitMQ bị gửi lại không tạo ra file báo cáo trùng lặp.
     *
     * @param jobId mã định danh của job xuất báo cáo
     */
    @Override
    public void export(String jobId) {
        ExportRequest job = exportRequestRepository.findByIdWithRequestedBy(jobId).orElse(null);
        if (job == null) {
            log.warn("Report export job not found: jobId={}", jobId);
            return;
        }
        if (!ExportRequestStatus.PENDING.name().equals(job.getStatus())) {
            log.info("Skipping report export job because it is not pending: jobId={}, status={}",
                    jobId, job.getStatus());
            return;
        }

        try {
            job.setStatus(ExportRequestStatus.RUNNING.name());
            job.setStartedAt(LocalDateTime.now());
            exportRequestRepository.save(job);

            GeneratedReport generatedReport = generateReport(job);
            String filePath = reportStorageService.save(
                    generatedReport.bytes(),
                    generatedReport.folder(),
                    generatedReport.filename()
            );

            job.setFileUrl(filePath);
            job.setStatus(ExportRequestStatus.DONE.name());
            job.setCompletedAt(LocalDateTime.now());
            exportRequestRepository.save(job);
        } catch (Exception e) {
            log.error("Failed to export report job {}", jobId, e);
            job.setStatus(ExportRequestStatus.FAILED.name());
            job.setErrorMessage(limitError(e.getMessage()));
            job.setCompletedAt(LocalDateTime.now());
            exportRequestRepository.save(job);
        }
    }

    private GeneratedReport generateReport(ExportRequest job) {
        ReportType reportType = parseReportType(job.getReportType());
        return switch (reportType) {
            case INVOICE -> new GeneratedReport(
                    jasperReportService.generateInvoicePdf(buildInvoiceData(job)),
                    "invoice",
                    "invoice-" + job.getId() + ".pdf"
            );
            case DAILY_REVENUE -> new GeneratedReport(
                    jasperReportService.generateDailyRevenuePdf(buildDailyRevenueData(job)),
                    "daily-revenue",
                    "daily-revenue-" + job.getId() + ".pdf"
            );
            case PRODUCT_CATALOG -> new GeneratedReport(
                    jasperReportService.generateProductCatalogPdf(buildProductCatalogData(job)),
                    "product-catalog",
                    "product-catalog-" + job.getId() + ".pdf"
            );
        };
    }

    private ReportType parseReportType(String reportType) {
        try {
            return ReportType.valueOf(reportType);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported report type: " + reportType);
        }
    }

    /**
     * Tạo dữ liệu báo cáo hóa đơn từ filter của job export.
     * <p>
     * Method đọc orderId/orderCode từ filter, query đơn hàng và dòng hàng từ cơ sở
     * dữ liệu,
     * rồi map sang DTO đầu vào cho Jasper template.
     *
     * @param job export request chứa loại báo cáo và filter JSON
     * @return DTO dữ liệu đầu vào cho Jasper report template
     */
    private InvoiceReportDto buildInvoiceData(ExportRequest job) {
        InvoiceHeaderResult header = findInvoiceHeader(job);
        List<InvoiceItemResult> items = orderItemRepository.findInvoiceItemsByOrderId(header.getOrderId());
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Order has no items: " + header.getOrderId());
        }

        return invoiceReportMapper.toReportDto(header, items, resolveCashierName(job));
    }

    private String resolveCashierName(ExportRequest job) {
        if (job.getRequestedBy() == null || job.getRequestedBy().getFullName() == null
                || job.getRequestedBy().getFullName().isBlank()) {
            throw new IllegalArgumentException("Report requested user is missing");
        }
        return job.getRequestedBy().getFullName();
    }

    private InvoiceHeaderResult findInvoiceHeader(ExportRequest job) {
        String orderId = readFilter(job.getFilters(), "orderId", null);
        if (orderId != null && !orderId.isBlank()) {
            return orderRepository.findInvoiceHeaderResultByOrderId(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        }

        String orderCode = readFilter(job.getFilters(), "orderCode", null);
        if (orderCode != null && !orderCode.isBlank()) {
            return orderRepository.findInvoiceHeaderByOrderCode(orderCode)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderCode));
        }

        throw new IllegalArgumentException("Invoice export requires orderId or orderCode filter");
    }

    private DailyRevenueReportDto buildDailyRevenueData(ExportRequest job) {
        String branchId = resolveBranchId(job);
        LocalDate fromDate = readLocalDateFilter(job.getFilters(), "fromDate", LocalDate.now());
        LocalDate toDate = readLocalDateFilter(job.getFilters(), "toDate", fromDate);
        if (toDate.isBefore(fromDate)) {
            throw new IllegalArgumentException("toDate must be greater than or equal to fromDate");
        }

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTime = toDate.plusDays(1).atStartOfDay();
        DailyRevenueSummaryResult summary = orderRepository
                .summarizeDailyRevenue(branchId, fromDateTime, toDateTime)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found: " + branchId));
        List<DailyRevenuePaymentResult> payments = orderRepository.findDailyRevenuePaymentBreakdown(
                branchId,
                fromDateTime,
                toDateTime);

        return dailyRevenueReportMapper.toReportDto(summary, payments, fromDate, toDate);
    }

    private ProductCatalogReportDto buildProductCatalogData(ExportRequest job) {
        String status = normalizeFilter(readFilter(job.getFilters(), "status", null));
        String categoryId = normalizeFilter(readFilter(job.getFilters(), "categoryId", null));
        List<ProductCatalogResult> products = productRepository.findProductCatalogReport(status, categoryId);
        return productCatalogReportMapper.toReportDto(products, status, categoryId);
    }

    private String resolveBranchId(ExportRequest job) {
        if (job.getBranch() != null && job.getBranch().getId() != null && !job.getBranch().getId().isBlank()) {
            return job.getBranch().getId();
        }
        String branchId = readFilter(job.getFilters(), "branchId", null);
        if (branchId != null && !branchId.isBlank()) {
            return branchId;
        }
        throw new IllegalArgumentException("Daily revenue export requires branchId");
    }

    /**
     * Đọc một field dạng chuỗi từ JSON filter được lưu trên job báo cáo.
     *
     * @param filters      JSON filter được gửi lên khi tạo job
     * @param field        tên field cần đọc
     * @param defaultValue giá trị mặc định khi filter thiếu hoặc không hợp lệ
     * @return giá trị của field hoặc {@code defaultValue}
     */
    private String readFilter(String filters, String field, String defaultValue) {
        if (filters == null || filters.isBlank()) {
            return defaultValue;
        }
        try {
            JsonNode node = objectMapper.readTree(filters).get(field);
            return node == null || node.isNull() ? defaultValue : node.asText(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private LocalDate readLocalDateFilter(String filters, String field, LocalDate defaultValue) {
        String value = readFilter(filters, field, null);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date filter " + field + ": " + value);
        }
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value)) {
            return null;
        }
        return value;
    }

    /**
     * Rút gọn thông báo lỗi trước khi lưu vào bản ghi export request.
     *
     * @param message thông báo gốc từ exception
     * @return thông báo không rỗng và không dài quá 500 ký tự
     */
    private String limitError(String message) {
        if (message == null || message.isBlank()) {
            return "Unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private record GeneratedReport(byte[] bytes, String folder, String filename) {
    }
}
