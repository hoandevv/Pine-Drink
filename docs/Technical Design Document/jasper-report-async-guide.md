# Hướng Dẫn Xuất Báo Cáo Bằng JasperReports Async

Tài liệu này mô tả cách tự tùy chỉnh JasperReports trong Pine Drink khi không tải được Jaspersoft Studio. Hướng đi chính: viết JRXML bằng tay, Spring Boot sinh PDF/XLSX ở background, lưu file xuống local storage để demo, frontend polling trạng thái job.

## 1. Mục tiêu

- Dùng JasperReports runtime trong Spring Boot, không phụ thuộc Jaspersoft Studio.
- Tạo report job bất đồng bộ để request không bị block.
- Lưu lịch sử xuất trong bảng `rp_export_request` đã có sẵn.
- Lưu file kết quả xuống local storage trong giai đoạn demo.
- Hỗ trợ định dạng `PDF`, sau đó mở rộng `XLSX`, `CSV`.

## 2. Trạng thái dự án hiện tại

Dự án đã có nền tảng tốt cho export report:

- `pom.xml` đã có Spring Boot 3.4.5, JPA, Web, Security, MinIO, RabbitMQ, Redis.
- DB migration đã có bảng `rp_export_request` trong `src/main/resources/db/migration/V8__create_nt_rp_pf_schema.sql`.
- Entity đã có `ExportRequest` tại `src/main/java/com/hoandev/pinedrink/entity/ExportRequest.java`.
- Repository đã có `ExportRequestRepository`.
- Enum đã có `ReportFileFormat` gồm `XLSX`, `CSV`, `PDF`.
- Storage đã có `FileStorageService` và `MinioFileStorageService`, nhưng demo dùng local storage riêng cho report.

Nhưng còn thiếu các phần sau:

- JasperReports dependencies.
- Field `status` trong `ExportRequest` entity.
- Enum trạng thái cho export job.
- Async config: `@EnableAsync`, `ThreadPoolTaskExecutor`.
- Service sinh báo cáo bằng Jasper.
- Service tạo job và worker async.
- Controller tạo job, lấy trạng thái, download file.
- Thư mục `src/main/resources/reports` chưa có template `.jrxml`.
- Lưu file từ `byte[]` hoặc `InputStream` xuống local folder.

## 3. Báo cáo nên làm trước

Nên làm theo thứ tự ưu tiên nghiệp vụ:

1. Hóa đơn PDF
   - Mục đích: in hóa đơn, gửi khách, xem trước đơn hàng.
   - Dữ liệu: chi nhánh, thu ngân, khách hàng, đơn hàng, sản phẩm, thanh toán, giảm giá, tổng tiền.
   - Định dạng: `PDF`.

2. Báo cáo doanh thu ngày/tháng
   - Mục đích: tổng kết doanh thu ngày/tháng.
   - Dữ liệu: số đơn, doanh thu gross, giảm giá, doanh thu net, phân loại thanh toán.
   - Định dạng: `PDF` cho tổng hợp, `XLSX` cho kế toán.

3. Báo cáo sản phẩm bán chạy
   - Mục đích: xem món bán chạy.
   - Dữ liệu: tên sản phẩm, danh mục, số lượng bán, doanh thu.
   - Định dạng: `XLSX`, có thể thêm `PDF`.

4. Báo cáo đối soát thanh toán
   - Mục đích: đối soát tiền mặt/chuyển khoản/ví điện tử.
   - Dữ liệu: phương thức thanh toán, thành công, thất bại, hoàn tiền, tổng tiền.
   - Định dạng: `XLSX`.

5. Báo cáo tồn kho
   - Mục đích: theo dõi tồn kho/cảnh báo sắp hết.
   - Dữ liệu: mã SKU, sản phẩm, tồn kho hiện tại, tồn kho tối thiểu, số lượng nhập, số lượng bán.
   - Định dạng: `XLSX`.

MVP nên chọn:

```text
Hóa đơn PDF + Báo cáo doanh thu PDF/XLSX
```

## 4. Kiến trúc tổng quan

```text
Frontend
  -> POST /api/reports/jobs
  <- jobId, status=PENDING

Frontend polling
  -> GET /api/reports/jobs/{id}
  <- status=PENDING|RUNNING|DONE|FAILED, fileUrl

Worker async
  -> tải ExportRequest
  -> truy vấn DB
  -> ánh xạ DTO
  -> điền JRXML
  -> xuất PDF/XLSX
  -> lưu file local
  -> cập nhật trạng thái DONE/FAILED
```

Request thread chỉ tạo job và trả về. Worker riêng xử lý report. Vì dự án đang demo và chưa cần nhiều instance, local storage là lựa chọn đơn giản nhất.

## 4.1. Quyết định storage cho demo

Trong giai đoạn demo, dùng local storage thay vì MinIO:

```text
storage/reports/
  invoice/
    invoice-{jobId}.pdf
  daily-revenue/
    daily-revenue-{jobId}.pdf
```

Lý do:

- Dễ chạy local, không cần cấu hình MinIO.
- Không phụ thuộc service ngoài.
- Đủ cho một backend instance.
- Dễ debug vì file nằm ngay trong máy dev.

Giới hạn:

- Không phù hợp nhiều instance vì file nằm trên máy sinh report.
- Nếu chạy Docker, cần mount volume để không mất file khi restart container.
- Production nên chuyển sang MinIO/S3 hoặc shared storage.

## 5. DB model

Bảng hiện có:

```sql
CREATE TABLE rp_export_request (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NULL,
    requested_by CHAR(36) NOT NULL,
    report_type VARCHAR(80) NOT NULL,
    file_format VARCHAR(20) NOT NULL DEFAULT 'XLSX',
    filters JSON NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    file_url VARCHAR(500) NULL,
    error_message VARCHAR(500) NULL,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at DATETIME NULL,
    completed_at DATETIME NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL
);
```

Cần đồng bộ entity:

```java
@Enumerated(EnumType.STRING)
@Column(name = "status", nullable = false)
private ExportRequestStatus status = ExportRequestStatus.PENDING;
```

Enum gợi ý:

```java
package com.hoandev.pinedrink.entity.enums;

public enum ExportRequestStatus implements BaseEnum {
    PENDING("PENDING"),
    RUNNING("RUNNING"),
    DONE("DONE"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED");

    private final String value;

    ExportRequestStatus(String value) {
        this.value = value;
    }

    @Override
    public String getValue() {
        return value;
    }
}
```

## 6. Maven dependencies

Thêm vào `pom.xml`:

```xml
<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports</artifactId>
    <version>7.0.1</version>
</dependency>

<dependency>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-fonts</artifactId>
    <version>7.0.1</version>
</dependency>
```

Nếu gặp lỗi PDF exporter/font, có thể cần thêm tùy version:

```xml
<dependency>
    <groupId>com.github.librepdf</groupId>
    <artifactId>openpdf</artifactId>
    <version>1.3.39</version>
</dependency>
```

Nếu xuất XLSX:

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

Lưu ý:

- Jaspersoft Studio 7.0.6 không bắt buộc.
- Runtime nên pin version rõ ràng.
- Nếu JRXML tạo bằng Studio 7.0.6 lỗi với runtime 7.0.1, cần căn chỉnh version JasperReports runtime.

## 7. Thư mục report template

Tạo cấu trúc:

```text
src/main/resources/reports/
  invoice.jrxml
  daily-revenue.jrxml
  fonts/
```

Tên file nên theo report type:

```text
INVOICE -> reports/invoice.jrxml
DAILY_REVENUE -> reports/daily-revenue.jrxml
TOP_PRODUCTS -> reports/top-products.jrxml
```

## 8. JRXML hóa đơn tối giản

Tạo file `src/main/resources/reports/invoice.jrxml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
              xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
              name="invoice"
              pageWidth="595"
              pageHeight="842"
              columnWidth="555"
              leftMargin="20"
              rightMargin="20"
              topMargin="20"
              bottomMargin="20">

    <parameter name="branchName" class="java.lang.String"/>
    <parameter name="branchAddress" class="java.lang.String"/>
    <parameter name="orderCode" class="java.lang.String"/>
    <parameter name="customerName" class="java.lang.String"/>
    <parameter name="cashierName" class="java.lang.String"/>
    <parameter name="orderTime" class="java.lang.String"/>
    <parameter name="subtotal" class="java.lang.String"/>
    <parameter name="discount" class="java.lang.String"/>
    <parameter name="total" class="java.lang.String"/>

    <field name="productName" class="java.lang.String"/>
    <field name="quantity" class="java.lang.Integer"/>
    <field name="unitPrice" class="java.lang.String"/>
    <field name="lineTotal" class="java.lang.String"/>

    <title>
        <band height="120">
            <staticText>
                <reportElement x="0" y="0" width="555" height="30"/>
                <textElement textAlignment="Center">
                    <font size="18" isBold="true"/>
                </textElement>
                <text><![CDATA[HÓA ĐƠN BÁN HÀNG]]></text>
            </staticText>
            <textField>
                <reportElement x="0" y="35" width="555" height="20"/>
                <textElement textAlignment="Center"/>
                <textFieldExpression><![CDATA[$P{branchName}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="0" y="58" width="555" height="20"/>
                <textElement textAlignment="Center"/>
                <textFieldExpression><![CDATA[$P{branchAddress}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="0" y="90" width="260" height="20"/>
                <textFieldExpression><![CDATA["Mã đơn: " + $P{orderCode}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="300" y="90" width="255" height="20"/>
                <textFieldExpression><![CDATA["Thời gian: " + $P{orderTime}]]></textFieldExpression>
            </textField>
        </band>
    </title>

    <pageHeader>
        <band height="45">
            <textField>
                <reportElement x="0" y="0" width="260" height="20"/>
                <textFieldExpression><![CDATA["Khách hàng: " + $P{customerName}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="300" y="0" width="255" height="20"/>
                <textFieldExpression><![CDATA["Thu ngân: " + $P{cashierName}]]></textFieldExpression>
            </textField>
            <staticText>
                <reportElement x="0" y="25" width="235" height="20"/>
                <text><![CDATA[Sản phẩm]]></text>
            </staticText>
            <staticText>
                <reportElement x="235" y="25" width="70" height="20"/>
                <textElement textAlignment="Right"/>
                <text><![CDATA[SL]]></text>
            </staticText>
            <staticText>
                <reportElement x="305" y="25" width="120" height="20"/>
                <textElement textAlignment="Right"/>
                <text><![CDATA[Đơn giá]]></text>
            </staticText>
            <staticText>
                <reportElement x="425" y="25" width="130" height="20"/>
                <textElement textAlignment="Right"/>
                <text><![CDATA[Thành tiền]]></text>
            </staticText>
        </band>
    </pageHeader>

    <detail>
        <band height="22">
            <textField>
                <reportElement x="0" y="0" width="235" height="20"/>
                <textFieldExpression><![CDATA[$F{productName}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="235" y="0" width="70" height="20"/>
                <textElement textAlignment="Right"/>
                <textFieldExpression><![CDATA[$F{quantity}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="305" y="0" width="120" height="20"/>
                <textElement textAlignment="Right"/>
                <textFieldExpression><![CDATA[$F{unitPrice}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="425" y="0" width="130" height="20"/>
                <textElement textAlignment="Right"/>
                <textFieldExpression><![CDATA[$F{lineTotal}]]></textFieldExpression>
            </textField>
        </band>
    </detail>

    <summary>
        <band height="90">
            <textField>
                <reportElement x="355" y="10" width="200" height="20"/>
                <textElement textAlignment="Right"/>
                <textFieldExpression><![CDATA["Tạm tính: " + $P{subtotal}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="355" y="35" width="200" height="20"/>
                <textElement textAlignment="Right"/>
                <textFieldExpression><![CDATA["Giảm giá: " + $P{discount}]]></textFieldExpression>
            </textField>
            <textField>
                <reportElement x="355" y="60" width="200" height="25"/>
                <textElement textAlignment="Right">
                    <font size="14" isBold="true"/>
                </textElement>
                <textFieldExpression><![CDATA["Tổng cộng: " + $P{total}]]></textFieldExpression>
            </textField>
        </band>
    </summary>
</jasperReport>
```

Lưu ý: ban đầu có thể dùng text không dấu để tránh lỗi font. Sau khi cấu hình font tiếng Việt xong, đổi lại thành có dấu.

## 9. DTO cho hóa đơn

Không đưa entity trực tiếp vào Jasper. Nên dùng DTO riêng.

```java
package com.hoandev.pinedrink.dto.report;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class InvoiceReportItemDto {
    private String productName;
    private Integer quantity;
    private String unitPrice;
    private String lineTotal;
}
```

```java
package com.hoandev.pinedrink.dto.report;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class InvoiceReportDto {
    private String branchName;
    private String branchAddress;
    private String orderCode;
    private String customerName;
    private String cashierName;
    private String orderTime;
    private String subtotal;
    private String discount;
    private String total;
    private List<InvoiceReportItemDto> items;
}
```

## 10. Jasper service

Service chịu trách nhiệm compile/fill/export.

```java
package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.dto.report.InvoiceReportDto;

public interface JasperReportService {
    byte[] generateInvoicePdf(InvoiceReportDto data);
}
```

```java
package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.dto.report.InvoiceReportDto;
import com.hoandev.pinedrink.service.JasperReportService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

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
            log.error("Lỗi khi sinh hóa đơn PDF", e);
            throw new IllegalStateException("Lỗi khi sinh hóa đơn PDF", e);
        }
    }
}
```

Tối ưu sau:

- Compile JRXML một lần khi app start, cache `JasperReport` trong memory.
- Chỉ compile lại khi template thay đổi trong dev.
- Production nên dùng `.jasper` precompiled nếu report nhiều.

## 11. Async config

Thêm vào main app hoặc config:

```java
@EnableAsync
@SpringBootApplication
public class PineDrinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(PineDrinkApplication.class, args);
    }
}
```

Tạo executor riêng:

```java
package com.hoandev.pinedrink.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class ReportAsyncConfig {

    @Bean("reportExecutor")
    public Executor reportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("report-");
        executor.initialize();
        return executor;
    }
}
```

## 12. Lưu byte[] xuống local storage

Demo không cần dùng `FileStorageService`. Tạo service riêng cho report:

```java
public interface ReportStorageService {
    String save(byte[] bytes, String relativeFolder, String filename);
    Resource load(String relativePath);
    Path resolve(String relativePath);
}
```

Config:

```yaml
app:
  report:
    storage:
      local-dir: ${REPORT_LOCAL_DIR:./storage/reports}
```

Local impl gợi ý:

```java
@Override
public String save(byte[] bytes, String relativeFolder, String filename) {
    try {
        Path folder = rootDir.resolve(relativeFolder).normalize();
        Files.createDirectories(folder);
        Path target = folder.resolve(filename).normalize();
        Files.write(target, bytes);
        return rootDir.relativize(target).toString().replace('\\', '/');
    } catch (IOException e) {
        throw new IllegalStateException("Lỗi khi lưu file báo cáo", e);
    }
}
```

Không lưu file trong `src/main/resources` hoặc `target`. Nên lưu ngoài source code bằng `./storage/reports`.

## 13. Report job service

Tách 2 service:

- `ReportJobService`: tạo job, lấy job, cập nhật trạng thái.
- `ReportExportWorker`: bất đồng bộ sinh file.

Request DTO:

```java
package com.hoandev.pinedrink.dto.request;

import com.hoandev.pinedrink.entity.enums.ReportFileFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportJobRequest {
    @NotBlank
    private String reportType;

    @NotNull
    private ReportFileFormat fileFormat;

    private String filters;
    private String branchId;
}
```

Response DTO:

```java
package com.hoandev.pinedrink.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReportJobResponse {
    private String id;
    private String reportType;
    private String fileFormat;
    private String status;
    private String fileUrl;
    private String errorMessage;
}
```

Service interface:

```java
package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.dto.request.CreateReportJobRequest;
import com.hoandev.pinedrink.dto.response.ReportJobResponse;

public interface ReportJobService {
    ReportJobResponse createJob(CreateReportJobRequest request, String requestedById);
    ReportJobResponse getJob(String jobId, String requestedById);
}
```

Worker interface:

```java
package com.hoandev.pinedrink.service;

public interface ReportExportWorker {
    void exportAsync(String jobId);
}
```

## 14. Worker async logic

Logic giả định:

```text
exportAsync(jobId)
  tải job
  nếu status != PENDING -> trả về
  đánh dấu RUNNING + startedAt
  switch reportType
    INVOICE -> xây dựng invoice DTO -> generateInvoicePdf
    DAILY_REVENUE -> xây dựng revenue DTO -> generate
  lưu kết quả xuống local storage
  đánh dấu DONE + completedAt + fileUrl
catch ex
  đánh dấu FAILED + errorMessage + completedAt
```

Code khung:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportExportWorkerImpl implements ReportExportWorker {

    private final ExportRequestRepository exportRequestRepository;
    private final JasperReportService jasperReportService;
    private final ReportStorageService reportStorageService;
    private final InvoiceReportDataService invoiceReportDataService;

    @Async("reportExecutor")
    @Transactional
    @Override
    public void exportAsync(String jobId) {
        ExportRequest job = exportRequestRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy export job"));

        try {
            job.setStatus(ExportRequestStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            exportRequestRepository.save(job);

            byte[] fileBytes;
            String filename;

            if ("INVOICE".equals(job.getReportType())) {
                InvoiceReportDto data = invoiceReportDataService.buildInvoiceData(job.getFilters());
                fileBytes = jasperReportService.generateInvoicePdf(data);
                filename = "invoice-" + job.getId() + ".pdf";
            } else {
                throw new IllegalArgumentException("Loại report chưa hỗ trợ: " + job.getReportType());
            }

            String fileUrl = reportStorageService.save(
                    fileBytes,
                    "invoice",
                    filename
            );

            job.setFileUrl(fileUrl);
            job.setStatus(ExportRequestStatus.DONE);
            job.setCompletedAt(LocalDateTime.now());
            exportRequestRepository.save(job);
        } catch (Exception e) {
            log.error("Lỗi khi xuất report job {}", jobId, e);
            job.setStatus(ExportRequestStatus.FAILED);
            job.setErrorMessage(limitError(e.getMessage()));
            job.setCompletedAt(LocalDateTime.now());
            exportRequestRepository.save(job);
        }
    }

    private String limitError(String message) {
        if (message == null) {
            return "Lỗi không xác định";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
```

Quan trọng: nếu dùng `@Transactional` ở worker, cần đảm bảo trạng thái RUNNING được flush trước khi generate lâu. Tốt hơn là tách method cập nhật trạng thái thành transaction riêng.

## 15. Controller API

Endpoints gợi ý:

```text
POST /api/reports/jobs
GET /api/reports/jobs/{id}
GET /api/reports/jobs/{id}/download
```

Controller khung:

```java
@RestController
@RequestMapping("/api/reports/jobs")
@RequiredArgsConstructor
public class ReportJobController {

    private final ReportJobService reportJobService;

    @PostMapping
    public ResponseEntity<ReportJobResponse> createJob(@Valid @RequestBody CreateReportJobRequest request) {
        String accountId = SecurityUtils.getCurrentAccountId();
        ReportJobResponse response = reportJobService.createJob(request, accountId);
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportJobResponse> getJob(@PathVariable String id) {
        String accountId = SecurityUtils.getCurrentAccountId();
        return ResponseEntity.ok(reportJobService.getJob(id, accountId));
    }
}
```

Download endpoint nên stream file từ local disk và luôn kiểm tra quyền user trước khi trả file.

## 16. API contract

Tạo job hóa đơn:

```http
POST /api/reports/jobs
Content-Type: application/json

{
  "reportType": "INVOICE",
  "fileFormat": "PDF",
  "filters": "{\"orderId\":\"ORDER_ID_HERE\"}"
}
```

Response:

```json
{
  "id": "job-id",
  "reportType": "INVOICE",
  "fileFormat": "PDF",
  "status": "PENDING",
  "fileUrl": null,
  "errorMessage": null
}
```

Polling:

```http
GET /api/reports/jobs/{id}
```

Phản hồi khi xong:

```json
{
  "id": "job-id",
  "reportType": "INVOICE",
  "fileFormat": "PDF",
  "status": "DONE",
  "fileUrl": "invoice/invoice-job-id.pdf",
  "errorMessage": null
}
```

Phản hồi khi lỗi:

```json
{
  "id": "job-id",
  "reportType": "INVOICE",
  "fileFormat": "PDF",
  "status": "FAILED",
  "fileUrl": null,
  "errorMessage": "Lỗi khi sinh hóa đơn PDF"
}
```

## 17. Frontend UX

Luồng:

```text
Người dùng bấm Xuất Hóa Đơn
  -> POST /api/reports/jobs
  -> hiển thị toast: Đang tạo báo cáo
  -> poll GET /api/reports/jobs/{id} mỗi 2s
  -> DONE: hiển thị nút Tải/Mở PDF
  -> FAILED: hiển thị lỗi + Thử lại
```

Quy tắc polling:

- Mỗi 2s trong 60s đầu.
- Sau 60s, mỗi 5s.
- Timeout UI sau 5 phút, nhưng job vẫn có thể chạy tiếp.
- Khi user reload page, có thể lấy lịch sử job gần nhất.

## 18. Bảo mật và phân quyền

- Chỉ user tạo job mới được xem/download job đó, trừ admin/manager.
- Không public thẳng thư mục local storage qua static resources.
- Download phải qua backend để check permission.
- Log audit: ai xuất report gì, lúc nào, chi nhánh nào.
- Không ghi dữ liệu nhạy cảm vào error_message.
- Giới hạn xuất đồng thời mỗi user, vì PDF có thể tốn CPU/RAM.

Quy tắc gợi ý:

```text
STAFF -> xuất hóa đơn của chi nhánh mình
MANAGER -> xuất báo cáo doanh thu/chi nhánh của chi nhánh mình
ADMIN -> xuất tất cả báo cáo
```

## 19. Giới hạn và retry

Nên có giới hạn:

- Tối đa 3 job RUNNING/PENDING mỗi user.
- Tối đa khoảng ngày 31 cho revenue report ban đầu.
- Tối đa số dòng cho XLSX, vì file quá lớn sẽ tốn memory.
- Dọn file sau 7-30 ngày.

Retry:

- MVP: không retry tự động, cho user bấm thử lại.
- Production: retry 1-3 lần với lỗi tạm thời như storage timeout.

Cancel:

- Cho cancel khi job còn `PENDING`.
- Không cần cancel khi `RUNNING` trong MVP.

## 20. Font tiếng Việt

Lúc đầu có thể dùng text không dấu để tránh lỗi. Khi cần tiếng Việt:

- Dùng font hỗ trợ Unicode: `DejaVu Sans`, `Noto Sans`, `Arial Unicode MS`.
- Embed font vào JasperReports.
- Đảm bảo PDF xuất không bị lỗi dấu tiếng Việt.

Hướng dẫn cài đặt cơ bản:

```text
src/main/resources/reports/fonts/
  DejaVuSans.ttf
  DejaVuSans-Bold.ttf
  jasperreports_extension.properties
  fonts.xml
```

`jasperreports_extension.properties`:

```properties
net.sf.jasperreports.extension.registry.factory.fonts=net.sf.jasperreports.engine.fonts.SimpleFontExtensionsRegistryFactory
net.sf.jasperreports.extension.simple.font.families.dejavu=reports/fonts/fonts.xml
```

`fonts.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<fontFamilies>
    <fontFamily name="DejaVu Sans">
        <normal>reports/fonts/DejaVuSans.ttf</normal>
        <bold>reports/fonts/DejaVuSans-Bold.ttf</bold>
        <pdfEncoding>Identity-H</pdfEncoding>
        <pdfEmbedded>true</pdfEmbedded>
    </fontFamily>
</fontFamilies>
```

Trong JRXML:

```xml
<font fontName="DejaVu Sans" size="12"/>
```

## 21. Lỗi hay gặp

1. `JRException: Error compiling report`
   - Sai XML schema, sai field/parameter name, sai expression.
   - Kiểm tra lại `$F{}` và `$P{}` có trùng DTO/params không.

2. PDF lỗi font tiếng Việt
   - Chưa embed font Unicode.
   - Dùng `Identity-H`, `pdfEmbedded=true`.

3. Report trắng
   - DataSource rỗng.
   - Detail band không có field.
   - Sai getter DTO. Field `productName` cần getter `getProductName()`.

4. Async không chạy
   - Thiếu `@EnableAsync`.
   - Gọi method `@Async` trong cùng class sẽ không qua proxy.
   - Method async phải nằm ở bean khác.

5. Trạng thái bị PENDING mãi
   - Worker lỗi trước khi cập nhật trạng thái.
   - Kiểm tra app log.
   - Đảm bảo `exportAsync(jobId)` được gọi sau khi save job.

6. Local storage fail
   - Sai quyền ghi folder.
   - Folder không tồn tại và app không được phép tạo.
   - Đường dẫn bị cấu hình sai.

## 22. Test plan

Unit/integration nên có:

- Sinh invoice PDF với data giả, assert `byte[].length > 0`.
- Tạo job thành công, trạng thái ban đầu `PENDING`.
- Worker thành công: `PENDING -> RUNNING -> DONE`, có `fileUrl`.
- Worker thất bại: `FAILED`, có `errorMessage`.
- User A không đọc/download job của user B.
- Loại report không hỗ trợ trả lời rõ ràng.

Test thủ công:

```text
1. Khởi động app
2. Đăng nhập lấy token
3. POST /api/reports/jobs với INVOICE/PDF
4. Poll GET /api/reports/jobs/{id}
5. Kiểm tra trạng thái DONE
6. Download/mở PDF
7. Kiểm tra file trong `storage/reports`
```

## 23. Lộ trình triển khai để ít lỗi

Thứ tự nên làm:

1. Thêm Jasper dependencies.
2. Thêm `ExportRequestStatus` và field `status` vào `ExportRequest`.
3. Thêm `@EnableAsync` và `ReportAsyncConfig`.
4. Thêm `ReportStorageService` lưu file local.
5. Tạo `reports/invoice.jrxml` bản tối giản.
6. Tạo DTO invoice report.
7. Tạo `JasperReportService.generateInvoicePdf`.
8. Tạo `InvoiceReportDataService` map order -> DTO.
9. Tạo `ReportJobService.createJob/getJob`.
10. Tạo `ReportExportWorker.exportAsync`.
11. Tạo controller `POST/GET`.
12. Test bằng Postman.
13. Thêm font tiếng Việt.
14. Mở rộng sang Báo cáo doanh thu.

## 24. Hướng mở rộng production

Sau MVP, nếu cần ổn định hơn:

- Dùng RabbitMQ queue thay vì `@Async` để scale multi-instance.
- Chuyển local storage sang MinIO/S3 hoặc shared storage.
- Thêm scheduler quét job `PENDING` bị kẹt.
- Thêm distributed lock bằng Redis khi multi-instance.
- Thêm retry_count, max_retries vào DB nếu cần retry auto.
- Precompile `.jrxml` thành `.jasper` khi build.
- Lưu objectName riêng thay vì chỉ lưu `fileUrl` để download/delete dễ hơn.
- Thêm `expires_at` để dọn file cũ.

## 25. Quyết định MVP cho Pine Drink

Nên chốt MVP như sau:

```text
Loại report: INVOICE
Định dạng: PDF
Template: src/main/resources/reports/invoice.jrxml viết tay
Job store: rp_export_request
Async: @Async("reportExecutor")
Storage: local folder ./storage/reports
Frontend: polling trạng thái + download khi DONE
```

Sau khi hóa đơn chạy ổn, làm tiếp:

```text
DAILY_REVENUE -> PDF/XLSX
TOP_PRODUCTS -> XLSX
PAYMENT_RECONCILIATION -> XLSX
```
