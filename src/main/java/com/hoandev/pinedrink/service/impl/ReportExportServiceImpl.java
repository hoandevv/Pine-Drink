package com.hoandev.pinedrink.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoandev.pinedrink.entity.ExportRequest;
import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportDto;
import com.hoandev.pinedrink.entity.enums.ExportRequestStatus;
import com.hoandev.pinedrink.entity.enums.ReportType;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ProductCatalogReportMapper;
import com.hoandev.pinedrink.repository.ExportRequestRepository;
import com.hoandev.pinedrink.repository.ProductRepository;
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

    private static final long MAX_REPORT_RANGE_DAYS = 366;

    private final ExportRequestRepository exportRequestRepository;
    private final JasperReportService jasperReportService;
    private final ReportStorageService reportStorageService;
    private final ObjectMapper objectMapper;
    private final ProductRepository productRepository;
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
     * Thực hiện query và lấy dữ liệu cho báo cáo.
     *
     * @param job job xuất báo cáo
     * @return dữ liệu báo cáo danh mục sản phẩm
     */
    private ProductCatalogReportDto buildProductCatalogData(ExportRequest job) {
        String status = normalizeFilter(readFilter(job.getFilters(), "status", null));
        String categoryId = normalizeFilter(readFilter(job.getFilters(), "categoryId", null));
        LocalDateTime fromDate = parseStartOfDay(readFilter(job.getFilters(), "fromDate", null));
        LocalDateTime toDate = parseExclusiveEndOfDay(readFilter(job.getFilters(), "toDate", null));
        validateDateRange(fromDate, toDate);
        List<ProductCatalogResult> products = productRepository.findProductCatalogReport(
                status, categoryId, fromDate, toDate);
        return productCatalogReportMapper.toReportDto(products, status, categoryId);
    }

    private LocalDateTime parseStartOfDay(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value).atStartOfDay();
    }

    private LocalDateTime parseExclusiveEndOfDay(String value) {
        return value == null || value.isBlank() ? null : LocalDate.parse(value).plusDays(1).atStartOfDay();
    }

    private void validateDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        LocalDate today = LocalDate.now();

        if (fromDate != null && toDate != null && !fromDate.isBefore(toDate)) {
            throw new BaseException(ErrorCode.REPORT_004);
        }
        if (fromDate != null && fromDate.toLocalDate().isAfter(today)) {
            throw new BaseException(ErrorCode.REPORT_005);
        }

        if (toDate != null && toDate.toLocalDate().minusDays(1).isAfter(today)) {
            throw new BaseException(ErrorCode.REPORT_006);
        }
        if (fromDate != null && toDate != null && java.time.Duration.between(fromDate, toDate).toDays() > MAX_REPORT_RANGE_DAYS) {
            throw new BaseException(ErrorCode.REPORT_007);
        }
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
