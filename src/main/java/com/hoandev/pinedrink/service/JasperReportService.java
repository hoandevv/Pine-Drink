package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.report.ProductCatalogReportDto;

/**
 * Service dùng để render các template JasperReports.
 * <p>
 * Service này chuyển các DTO báo cáo đã hợp lệ thành file nhị phân. Service
 * không quyết định file được lưu ở đâu hoặc job được đưa vào queue như thế nào.
 */
public interface JasperReportService {

    /**
     * Sinh file PDF danh mục sản phẩm.
     *
     * @param data danh sách sản phẩm đã map cho Jasper template
     * @return nội dung PDF dạng byte
     */
    byte[] generateProductCatalogPdf(ProductCatalogReportDto data);
}
