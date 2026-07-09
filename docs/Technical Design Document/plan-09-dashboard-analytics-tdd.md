# Plan 09 - Dashboard Analytics Technical Design

## Goal
Triển khai dashboard admin v1 theo hướng nhanh, nhẹ, dễ mở rộng. Backend dùng stored procedure để tính aggregate, API chỉ trả DTO cho frontend render biểu đồ.

## Scope
Trong phạm vi:
- KPI tổng quan: doanh thu, đơn hoàn tất, đơn hủy, AOV, khách mới.
- Doanh thu theo ngày.
- Đơn hàng theo trạng thái.
- Top sản phẩm bán chạy.
- Hiệu suất chi nhánh.
- Filter theo `fromDate`, `toDate`, optional `branchId`.

Ngoài phạm vi:
- Realtime dashboard.
- Drill-down chi tiết từng đơn.
- Export PDF/Excel dashboard.
- Dự báo doanh thu, AI insight.

## Current Context
- Order data nằm trong `od_order`, `od_order_item`, `od_order_item_topping`.
- Payment data nằm trong `py_transaction`, `py_payment_intent`.
- Product data nằm trong `pr_product`, `pr_product_variant`, `pr_category`.
- Customer data nằm trong `cu_customer_profile`.
- Doanh thu chỉ tính từ đơn `COMPLETED`.
- Dashboard hiện chưa có controller/service/repository riêng.

## Dashboard Widgets v1
| Widget | Mục đích | Data source |
| --- | --- | --- |
| Overview cards | Xem nhanh sức khỏe kinh doanh | `od_order`, `cu_customer_profile` |
| Revenue trend | Theo dõi doanh thu theo ngày | `od_order` |
| Order status | Theo dõi backlog/vận hành đơn | `od_order` |
| Top products | Biết sản phẩm bán chạy | `od_order`, `od_order_item` |
| Branch performance | So sánh hiệu suất chi nhánh | `od_order`, `ce_branch` |

## API

Base path:

```text
/api/v1/admin/dashboard
```

Authorization:
- `PERM_REPORT_VIEW`
- Scope branch qua `accessScopeService` nếu có `branchId`.

### Overview

```text
GET /api/v1/admin/dashboard/overview?fromDate=2026-07-01&toDate=2026-07-09&branchId={uuid}
```

Response:

```json
{
  "totalRevenue": 12500000,
  "completedOrders": 210,
  "cancelledOrders": 12,
  "averageOrderValue": 59523,
  "newCustomers": 34
}
```

### Revenue Trend

```text
GET /api/v1/admin/dashboard/revenue-trend?fromDate=2026-07-01&toDate=2026-07-09&branchId={uuid}
```

Response:

```json
[
  {
    "date": "2026-07-01",
    "revenue": 1500000,
    "orders": 25,
    "averageOrderValue": 60000
  }
]
```

### Order Status

```text
GET /api/v1/admin/dashboard/order-status?fromDate=2026-07-01&toDate=2026-07-09&branchId={uuid}
```

Response:

```json
[
  {
    "status": "COMPLETED",
    "count": 210
  }
]
```

### Top Products

```text
GET /api/v1/admin/dashboard/top-products?fromDate=2026-07-01&toDate=2026-07-09&branchId={uuid}&limit=10
```

Response:

```json
[
  {
    "productId": "uuid",
    "productName": "Tra sua tran chau",
    "quantitySold": 120,
    "revenue": 4800000
  }
]
```

### Branch Performance

```text
GET /api/v1/admin/dashboard/branch-performance?fromDate=2026-07-01&toDate=2026-07-09
```

Response:

```json
[
  {
    "branchId": "uuid",
    "branchName": "Pine Drink Quan 1",
    "revenue": 6200000,
    "completedOrders": 105,
    "cancelledOrders": 5,
    "averageOrderValue": 59047
  }
]
```

## Stored Procedures

Migration mới:

```text
src/main/resources/db/migration/V16__create_dashboard_procedures.sql
```

Procedures:

```sql
sp_dashboard_overview(IN p_from_date DATE, IN p_to_date DATE, IN p_branch_id CHAR(36))
sp_dashboard_revenue_trend(IN p_from_date DATE, IN p_to_date DATE, IN p_branch_id CHAR(36))
sp_dashboard_order_status(IN p_from_date DATE, IN p_to_date DATE, IN p_branch_id CHAR(36))
sp_dashboard_top_products(IN p_from_date DATE, IN p_to_date DATE, IN p_branch_id CHAR(36), IN p_limit INT)
sp_dashboard_branch_performance(IN p_from_date DATE, IN p_to_date DATE)
```

Quy ước:
- Date filter dùng range `[fromDate 00:00:00, toDate + 1 day 00:00:00)`.
- `p_branch_id IS NULL` nghĩa là xem toàn hệ thống.
- Revenue chỉ tính `status = 'COMPLETED'`.
- Cancel count tính `status = 'CANCELLED'`.
- Top products chỉ tính item thuộc order `COMPLETED`.

## Backend Design

Luồng:

```text
DashboardController -> DashboardService -> DashboardRepository -> CALL stored procedure -> DTO
```

Files cần tạo:
- `controller/DashboardController.java`
- `service/DashboardService.java`
- `service/impl/DashboardServiceImpl.java`
- `repository/DashboardRepository.java`
- `repository/impl/DashboardRepositoryImpl.java`
- `entity/dto/response/Dashboard/DashboardOverviewResponse.java`
- `entity/dto/response/Dashboard/RevenueTrendResponse.java`
- `entity/dto/response/Dashboard/OrderStatusSummaryResponse.java`
- `entity/dto/response/Dashboard/TopProductResponse.java`
- `entity/dto/response/Dashboard/BranchPerformanceResponse.java`

Repository dùng `EntityManager` native query:

```java
entityManager
    .createNativeQuery("CALL sp_dashboard_overview(:fromDate, :toDate, :branchId)")
    .setParameter("fromDate", fromDate)
    .setParameter("toDate", toDate)
    .setParameter("branchId", branchId)
    .getResultList();
```

## Validation Rules
- `fromDate` bắt buộc.
- `toDate` bắt buộc.
- `fromDate <= toDate`.
- Khoảng thời gian tối đa v1: 366 ngày.
- `limit` top products mặc định `10`, tối đa `50`.
- User không có quyền xem toàn hệ thống thì bắt buộc filter theo branch được phép.

## Performance
- Query aggregate chạy trong DB, không load entity JPA.
- API response nhỏ, không pagination cho v1.
- Cache Redis optional: key theo endpoint + filters, TTL 60 giây.
- Không join bảng product/category nếu `od_order_item` đã có snapshot `product_name` đủ dùng.

Index khuyến nghị thêm nếu dữ liệu lớn:

```sql
CREATE INDEX idx_od_order_status_created_branch ON od_order(status, created_at, branch_id);
CREATE INDEX idx_od_item_order_product ON od_order_item(order_id, product_id);
CREATE INDEX idx_cu_customer_created ON cu_customer_profile(created_at);
```

## Implementation Steps
1. Tạo migration `V16__create_dashboard_procedures.sql`.
2. Tạo dashboard response DTOs.
3. Tạo `DashboardRepository` gọi stored procedure.
4. Tạo `DashboardService` validate filter + check access scope.
5. Tạo `DashboardController` với 5 endpoint.
6. Seed permission `PERM_REPORT_VIEW` nếu thiếu.
7. Test procedure bằng dữ liệu seed/report hiện có.
8. Tích hợp frontend dashboard v1.

## Test Plan
- Unit test service validation date range.
- Repository integration test gọi đủ 5 stored procedures.
- API test quyền `PERM_REPORT_VIEW`.
- Case `branchId = null` trả toàn hệ thống cho admin.
- Case có `branchId` chỉ trả dữ liệu branch đó.
- Case không có data trả `0` hoặc list rỗng, không lỗi.

## Follow-up
- Thêm payment success/failure analytics.
- Thêm inventory warning từ daily stock.
- Thêm customer retention/new vs returning.
- Thêm export dashboard qua report module.
- Thêm realtime refresh qua WebSocket.
