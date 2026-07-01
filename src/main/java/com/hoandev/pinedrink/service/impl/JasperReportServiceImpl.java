package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.dto.report.InvoiceReportDto;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.JasperReportService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
/**Module cần xuất PDF
 ↓
 DTO chứa dữ liệu report
 ↓
 File .jrxml thiết kế giao diện report
 ↓
 Service JasperReport
 ↓
 Xuất PDF
 */
@Slf4j
@Service
public class JasperReportServiceImpl implements JasperReportService {

    @Override
    public byte[] generateInvoicePdf(InvoiceReportDto data) {
        try (InputStream template = new ClassPathResource("reports/invoice.jrxml").getInputStream()) {
            JasperReport report = JasperCompileManager.compileReport(template);

            Map<String, Object> params = new HashMap<>();
            params.put("branchName", data.getBranchName());
            params.put("branchAddress", data.getBranchAddress());
            params.put("orderCode", data.getOrderCode());
            params.put("customerName", data.getCustomerName());
            params.put("cashierName", data.getCashierName());
            params.put("orderTime", data.getOrderTime());
            params.put("subtotal", data.getSubtotal());
            params.put("discount", data.getDiscount());
            params.put("total", data.getTotal());

            JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(data.getItems());
            JasperPrint print = JasperFillManager.fillReport(report, params, dataSource);

            return JasperExportManager.exportReportToPdf(print);
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF", e);
            throw new BaseException(ErrorCode.COM_002, "Failed to generate invoice PDF");
        }
    }
}
