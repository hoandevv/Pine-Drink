# Plan 9 — Notification, Report & Admin Features

## Mục tiêu
Xây dựng Notification system (in-app + email), Report/Export module, Audit logging, và Admin dashboard.

## Files cần tạo

| File | Mô tả |
|------|-------|
| `service/NotificationService.java` | Interface NotificationService |
| `service/impl/NotificationServiceImpl.java` | Implementation |
| `service/ExportService.java` | Interface ExportService |
| `service/impl/ExportServiceImpl.java` | Implementation |
| `service/AuditService.java` | Interface AuditService |
| `service/impl/AuditServiceImpl.java` | Implementation |
| `controller/NotificationController.java` | REST controller cho notifications |
| `controller/ReportController.java` | REST controller cho reports/export |
| `controller/AdminController.java` | REST controller cho admin dashboard |

## Chi tiết implementation

### 1. Notification System

```java
@Entity
@Table(name = "nt_notification")
public class Notification {
    @Id
    private UUID id;

    private UUID userId;
    private String type;    // ORDER_CONFIRMED, ORDER_READY, PROMOTION, SYSTEM
    private String title;
    private String body;
    private String referenceType; // ORDER, PROMOTION...
    private UUID referenceId;

    private Boolean isRead;
    private LocalDateTime createdAt;
}
```

#### NotificationService

```java
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final JavaMailSender mailSender;
    private final NotificationTemplateRepository templateRepository;

    @Override
    @Transactional
    public void sendInAppNotification(UUID userId, CreateNotificationRequest request) {
        Notification notif = Notification.builder()
                .userId(userId)
                .type(request.type())
                .title(request.title())
                .body(request.body())
                .referenceType(request.referenceType())
                .referenceId(request.referenceId())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();
        notificationRepository.save(notif);

        // Push realtime qua WebSocket
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/queue/notifications",
            NotificationResponse.fromEntity(notif));
    }

    @Override
    public void sendEmail(UUID userId, String templateCode, Map<String, Object> params) {
        User user = userRepository.findById(userId).orElseThrow();
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow();

        String content = renderTemplate(template.getBody(), params);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(user.getEmail());
        message.setSubject(template.getSubject());
        message.setText(content);
        mailSender.send(message);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notif = notificationRepository.findById(notificationId)
                .orElseThrow();
        notif.setIsRead(true);
        notificationRepository.save(notif);
    }
}
```

### 2. Report/Export — XLSX with Apache POI

```java
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final OrderRepository orderRepository;
    private final ExportRequestRepository exportRequestRepository;
    private final MinioService minioService;

    @Override
    @Async
    public void exportOrders(ExportRequest request) {
        try {
            List<Order> orders = orderRepository
                .findByCreatedAtBetween(request.getFromDate(), request.getToDate());

            Workbook workbook = new XSSFWorkbook();
            Sheet sheet = workbook.createSheet("Orders");

            // Header row
            Row header = sheet.createRow(0);
            String[] columns = {"Order Code", "Branch", "Status", "Total", "Created At"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Data rows
            int rowNum = 1;
            for (Order order : orders) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(order.getOrderCode());
                row.createCell(1).setCellValue(order.getBranchId().toString());
                row.createCell(2).setCellValue(order.getStatus().name());
                row.createCell(3).setCellValue(order.getTotalAmount().doubleValue());
                row.createCell(4).setCellValue(order.getCreatedAt().toString());
            }

            // Write to byte array
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();

            // Upload lên MinIO
            String fileName = "orders_export_" + LocalDateTime.now() + ".xlsx";
            String url = minioService.uploadFile(fileName, out.toByteArray(), "xlsx");

            // Update export request status
            request.setStatus(ExportRequestStatus.COMPLETED);
            request.setFileUrl(url);
            exportRequestRepository.save(request);

            // Notify user
            notificationService.sendInAppNotification(request.getCreatedBy(),
                CreateNotificationRequest.builder()
                    .type("EXPORT_READY")
                    .title("Export hoàn tất")
                    .body("File export orders đã sẵn sàng: " + fileName)
                    .referenceType("EXPORT")
                    .referenceId(request.getId())
                    .build());

        } catch (Exception e) {
            request.setStatus(ExportRequestStatus.FAILED);
            request.setErrorMessage(e.getMessage());
            exportRequestRepository.save(request);
        }
    }

    @Override
    public void exportRevenueByBranch(LocalDate fromDate, LocalDate toDate) {
        // Tương tự exportOrders nhưng aggregate doanh thu theo branch
    }

    @Override
    public void exportInventory(UUID branchId) {
        // Export tồn kho hiện tại của branch
    }
}
```

### 3. Audit Log

```java
@Entity
@Table(name = "rp_audit_log")
public class AuditLog {
    @Id
    private UUID id;

    private String module;     // BRAND, PRODUCT, ORDER, PAYMENT, ACCOUNT
    private String action;     // CREATE, UPDATE, DELETE
    private UUID entityId;
    private String entityType;
    @Column(columnDefinition = "JSON")
    private String beforeData; // JSON trước khi change
    @Column(columnDefinition = "JSON")
    private String afterData;  // JSON sau khi change
    private UUID performedBy;
    private String ipAddress;
    private LocalDateTime createdAt;
}
```

#### AuditService — dùng @Aspect

```java
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;

    @AfterReturning(value = "@annotation(auditable)", returning = "result")
    public void logAudit(JoinPoint joinPoint, Auditable auditable, Object result) {
        // Lấy before/after data từ joinPoint args và result
        // Ghi audit log
        auditService.log(AuditLog.builder()
                .module(auditable.module())
                .action(auditable.action())
                .entityId(extractEntityId(result))
                .entityType(result.getClass().getSimpleName())
                .beforeData(getBeforeData(joinPoint))
                .afterData(toJson(result))
                .performedBy(getCurrentUserId())
                .ipAddress(getCurrentIp())
                .build());
    }
}
```

### 4. Admin Dashboard

```java
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        Long totalOrdersToday = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);
        BigDecimal revenueToday = orderRepository
            .sumTotalByStatusAndCreatedAtBetween(OrderStatus.COMPLETED, startOfDay, endOfDay);
        Long activeCustomers = userRepository.countActiveCustomers();

        return ResponseEntity.ok(ApiResponse.success(
            new DashboardSummary(totalOrdersToday, revenueToday, activeCustomers)));
    }

    @GetMapping("/dashboard/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopProductResponse> topProducts = orderItemRepository
            .findTopSellingProducts(PageRequest.of(0, limit));
        return ResponseEntity.ok(ApiResponse.success(topProducts));
    }
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/notifications?page=&size=` | Danh sách notification |
| PATCH | `/notifications/{id}/read` | Mark as read |
| PATCH | `/notifications/read-all` | Mark all as read |
| GET | `/reports/orders/export?fromDate=&toDate=` | Export orders XLSX |
| GET | `/reports/revenue/export?fromDate=&toDate=` | Export revenue XLSX |
| GET | `/reports/inventory/export?branchId=` | Export inventory XLSX |
| GET | `/audit-logs?module=&action=&fromDate=&toDate=` | Xem audit logs |
| GET | `/admin/dashboard/summary` | Thống kê tổng quan |
| GET | `/admin/dashboard/top-products?limit=` | Top sản phẩm bán chạy |

## Checklist

- [ ] Tạo Notification entity + repository
- [ ] Tạo NotificationService (in-app, email, WebSocket push)
- [ ] Tạo NotificationController (list, mark read)
- [ ] Cấu hình Spring Mail SMTP
- [ ] Tạo notification template entity
- [ ] Tạo ExportService với Apache POI
- [ ] Async export với @Async + ExportRequest tracking
- [ ] Upload file lên MinIO
- [ ] Notify user khi export hoàn tất
- [ ] Tạo AuditLog entity
- [ ] Tạo AuditAspect với @Auditable annotation
- [ ] Tạo AdminController dashboard endpoints
- [ ] Aggregate queries cho top products, revenue
