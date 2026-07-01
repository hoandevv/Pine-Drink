package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.report.DailyRevenueReportDto;
import com.hoandev.pinedrink.entity.dto.report.InvoiceReportDto;

/**
 * Service dùng để render các template JasperReports.
 * <p>
 * Service này chuyển các DTO báo cáo đã hợp lệ thành file nhị phân. Service
 * không quyết định file được lưu ở đâu hoặc job được đưa vào queue như thế nào.
 */
public interface JasperReportService {

    /**
     * Sinh file PDF hóa đơn từ DTO báo cáo hóa đơn.
     *
     * @param data thông tin header, tổng tiền và các dòng chi tiết cho Jasper template
     * @return nội dung PDF dạng byte
     */
    byte[] generateInvoicePdf(InvoiceReportDto data);

    /**
     * Sinh file PDF báo cáo doanh thu theo ngày/tháng.
     *
     * @param data thông tin tổng hợp doanh thu và breakdown theo phương thức thanh toán
     * @return nội dung PDF dạng byte
     */
    byte[] generateDailyRevenuePdf(DailyRevenueReportDto data);
}
