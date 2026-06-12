# Technical Design Document - Voucher CRUD

| Field | Value |
|---|---|
| Document ID | TDD-PINE-VOUCHER-CRUD-02.1 |
| Project | Pine Drink |
| Module | Voucher Management |
| Version | 1.0 |
| Status | Planned |
| Last Updated | 2026-06-10 |

---

## 1. Mục Đích

Tài liệu này mô tả thiết kế kỹ thuật cho nhóm API CRUD voucher trong backoffice Pine Drink.

Mục tiêu:
- Cho admin tạo voucher mới với rule giảm giá và thời gian hiệu lực.
- Cho admin cập nhật voucher và giới hạn chi nhánh áp dụng.
- Cho admin xem danh sách, xem chi tiết, bật/tắt trạng thái voucher.
- Chặn xoá voucher đã phát sinh lịch sử sử dụng để bảo toàn dữ liệu order.
- Tái sử dụng đúng schema hiện có cho flow cart/order đã thiết kế trước đó.

---

## 2. Bối Cảnh

Voucher hiện đã có schema và một phần logic validate/apply trong flow cart/order:
- `src/main/java/com/hoandev/pinedrink/entity/Voucher.java`
- `src/main/java/com/hoandev/pinedrink/entity/VoucherBranch.java`
- `src/main/java/com/hoandev/pinedrink/entity/VoucherUsage.java`
- `src/main/java/com/hoandev/pinedrink/repository/VoucherRepository.java`
- `src/main/java/com/hoandev/pinedrink/repository/VoucherBranchRepository.java`
- `src/main/java/com/hoandev/pinedrink/repository/VoucherUsageRepository.java`
- `docs/Technical Design Document/plan-03-cart-order-tdd.md`

Hiện tại hệ thống chưa có module backoffice đầy đủ để quản lý voucher master data:

```text
Admin muốn tạo/sửa voucher
-> chưa có VoucherController/VoucherService CRUD riêng
-> FE chưa có API chuẩn để quản lý voucher
-> dữ liệu voucher hiện chỉ được dùng gián tiếp ở cart/order flow
```

Phần này bổ sung lớp quản trị voucher để backend có thể vận hành campaign, cấu hình rule giảm giá và branch scope một cách nhất quán.

---

## 3. Phạm Vi Chức Năng

Functional scope:
- Tạo voucher.
- Cập nhật voucher.
- Cập nhật trạng thái voucher.
- Xoá voucher chưa có usage.
- Lấy chi tiết voucher.
- Lấy danh sách voucher có filter + phân trang.
- Quản lý branch scope qua `vc_voucher_branch`.

Out of scope:
- Apply voucher ở cart.
- Refund voucher usage khi cancel order.
- Voucher campaign analytics/reporting.
- Bulk import voucher.
- Phân loại voucher theo brand; hệ thống là branch-first, không có brand scope.

---

## 4. Mô Hình Dữ Liệu

Schema liên quan:

```text
vc_voucher
  id, code, name, description, discount_type, discount_value,
  max_discount_amount, min_order_amount,
  usage_limit, used_count, usage_limit_per_customer,
  start_at, end_at, status

vc_voucher_branch
  id, voucher_id, branch_id, status

vc_voucher_usage
  id, voucher_id, order_id, customer_id, discount_amount, used_at
```

Ý nghĩa nghiệp vụ:
- `vc_voucher`: định nghĩa voucher toàn cục.
- `vc_voucher_branch`: bảng giới hạn voucher theo chi nhánh; nếu không có row thì hiểu là áp dụng toàn hệ thống.
- `vc_voucher_usage`: lịch sử sử dụng voucher theo order/customer.

Entity hiện có:
- `Voucher` có các field chính: `code`, `name`, `description`, `discountType`, `discountValue`, `maxDiscountAmount`, `minOrderAmount`, `usageLimit`, `usedCount`, `usageLimitPerCustomer`, `startAt`, `endAt`.
- `VoucherBranch` map `voucher_id -> branch_id`.
- `VoucherUsage` map usage theo `voucher_id`, `order_id`, `customer_id`.

Ràng buộc quan trọng:
- `vc_voucher.code` là unique.
- `vc_voucher_branch(voucher_id, branch_id)` là unique.
- `vc_voucher_usage(voucher_id, order_id)` là unique.

---

## 5. Quy Tắc Nghiệp Vụ

- Voucher mặc định là global; nếu có branch restriction thì lưu vào `vc_voucher_branch`.
- `discountType` chỉ hỗ trợ `PERCENTAGE` và `FIXED_AMOUNT`.
- `minOrderAmount` mặc định là `0` nếu request không truyền lên.
- `usedCount` là field runtime của hệ thống, không cho phép FE tự sửa.
- `PERCENTAGE` phải nằm trong khoảng `0 < discountValue <= 100`.
- `FIXED_AMOUNT` phải có `discountValue > 0`; `maxDiscountAmount` không có ý nghĩa nghiệp vụ.
- `startAt` phải nhỏ hơn `endAt`.
- `usageLimit` và `usageLimitPerCustomer` nếu có thì phải >= 1.
- Voucher đã có usage không được xoá cứng.
- Disable voucher dùng `status`, không xóa để thay thế cho các trường hợp đã từng được tham chiếu trong nghiệp vụ.

---

## 6. Thiết Kế API

Base path:

```http
/api/v1/vouchers
```

Danh sách endpoint:

| Method | Path | Mô tả |
|---|---|---|
| `POST` | `/api/v1/vouchers` | Tạo voucher |
| `PUT` | `/api/v1/vouchers/{id}` | Cập nhật voucher |
| `PATCH` | `/api/v1/vouchers/{id}/status` | Cập nhật trạng thái |
| `DELETE` | `/api/v1/vouchers/{id}` | Xoá voucher |
| `GET` | `/api/v1/vouchers/{id}` | Lấy chi tiết voucher |
| `GET` | `/api/v1/vouchers` | Lấy danh sách voucher |

Filter gợi ý cho API danh sách:
- `keyword`
- `status`
- `discountType`
- `branchId`
- `activeAt`

Chữ ký service gợi ý:

```java
VoucherResponse create(CreateVoucherRequest request);
VoucherResponse update(String id, UpdateVoucherRequest request);
VoucherResponse updateStatus(String id, UpdateVoucherStatusRequest request);
void delete(String id);
VoucherResponse getById(String id);
PageResponse<VoucherResponse> getAll(String keyword, String status,
                                     String discountType, String branchId,
                                     LocalDateTime activeAt, Pageable pageable);
```

---

## 7. Request/Response Shape

### 7.1 Request create/update

```json
{
  "code": "SUMMER10",
  "name": "Summer 10%",
  "description": "Giảm giá cho chiến dịch mùa hè",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxDiscountAmount": 30000,
  "minOrderAmount": 99000,
  "usageLimit": 1000,
  "usageLimitPerCustomer": 2,
  "startAt": "2026-06-01T00:00:00",
  "endAt": "2026-06-30T23:59:59",
  "branchIds": [
    "branch-1",
    "branch-2"
  ]
}
```

Ý nghĩa:
- `branchIds=[]` hoặc `null` nghĩa là voucher áp dụng global.
- `code` nên được normalize uppercase trước khi save.

### 7.2 Request cập nhật trạng thái

```json
{
  "status": "INACTIVE"
}
```

### 7.3 Response chi tiết gợi ý

```json
{
  "id": "voucher-id",
  "code": "SUMMER10",
  "name": "Summer 10%",
  "description": "Giảm giá cho chiến dịch mùa hè",
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "maxDiscountAmount": 30000,
  "minOrderAmount": 99000,
  "usageLimit": 1000,
  "usedCount": 12,
  "usageLimitPerCustomer": 2,
  "startAt": "2026-06-01T00:00:00",
  "endAt": "2026-06-30T23:59:59",
  "status": "ACTIVE",
  "branchIds": [
    "branch-1",
    "branch-2"
  ]
}
```

---

## 8. Validation Và Error Rules

Validation input:
- `code` bắt buộc, trim khoảng trắng, uppercase-normalized, không trùng.
- `name`, `discountType`, `discountValue`, `startAt`, `endAt` là bắt buộc.
- `endAt` phải lớn hơn `startAt`.
- `discountValue > 0`.
- Với `PERCENTAGE`: `discountValue <= 100`.
- Với `FIXED_AMOUNT`: `maxDiscountAmount` có thể bỏ qua khi persist/mapping.
- `minOrderAmount >= 0`.
- `usageLimit >= 1` nếu có.
- `usageLimitPerCustomer >= 1` nếu có.
- `branchIds` không được trùng và tất cả phải tồn tại trong `ce_branch`.

Error cases chính:
- Tạo/cập nhật với `code` trùng -> reject.
- Request có `branchIds` chứa chi nhánh không tồn tại -> reject.
- `startAt >= endAt` -> reject.
- `discountType` ngoài enum hỗ trợ -> reject.
- Xoá voucher đã có row trong `vc_voucher_usage` -> reject.
- Cập nhật voucher không tồn tại -> trả not found.

---

## 9. Runtime Flow

### 9.1 Luồng create/update

```text
Admin gọi API create/update voucher
        |
        v
Controller validate request + check permission
        |
        v
VoucherService normalize code, validate business rules
        |
        +--> validate branchIds tồn tại
        |
        +--> check unique code
        |
        v
save vc_voucher
        |
        v
replace rows vc_voucher_branch theo branchIds mới
        |
        v
map VoucherResponse
```

### 9.2 Luồng get detail/list

```text
Request vào controller
        |
        v
query voucher data
        |
        v
load branch scope tương ứng
        |
        v
map response trả về FE
```

### 9.3 Luồng delete

```text
Admin gọi DELETE voucher
        |
        v
check voucher tồn tại
        |
        v
check vc_voucher_usage có row hay không
        |
        +--> có usage: reject delete
        |
        +--> chưa có usage: delete vc_voucher_branch -> delete vc_voucher
```

---

## 10. Repository Và Query Design

Repository hiện có:
- `VoucherRepository`
- `VoucherBranchRepository`
- `VoucherUsageRepository`

Repository methods hiện đang có:

```java
Optional<Voucher> findByCode(String code);
List<Voucher> findByStatus(String status);
List<VoucherBranch> findByVoucherId(String voucherId);
List<VoucherBranch> findByBranchId(String branchId);
List<VoucherUsage> findByVoucherId(String voucherId);
List<VoucherUsage> findByOrderId(String orderId);
List<VoucherUsage> findByCustomerIdAndVoucherId(String customerId, String voucherId);
```

Methods nên bổ sung cho CRUD:

```java
boolean existsByCode(String code);
boolean existsByCodeAndIdNot(String code, String id);
boolean existsByVoucherId(String voucherId);          // cho usage check
void deleteByVoucherId(String voucherId);             // xoá branch scope cũ
boolean existsByVoucherIdAndBranchId(String voucherId, String branchId);
```

Nếu API list cần filter linh hoạt, nên dùng một trong hai hướng:
- `JpaSpecificationExecutor<Voucher>` cho filter động.
- Custom repository query nếu muốn kiểm soát join `vc_voucher_branch` rõ ràng hơn.

---

## 11. Phân Quyền

Voucher là master data toàn hệ thống. Các API mutation yêu cầu permission voucher và system scope.

Permission gợi ý:

```text
PERM_VOUCHER_CREATE
PERM_VOUCHER_UPDATE
PERM_VOUCHER_DELETE
PERM_VOUCHER_VIEW
```

Controller gate:

```java
@PreAuthorize("hasAuthority('PERM_VOUCHER_CREATE')")
@PreAuthorize("hasAuthority('PERM_VOUCHER_UPDATE')")
@PreAuthorize("hasAuthority('PERM_VOUCHER_DELETE')")
@PreAuthorize("hasAuthority('PERM_VOUCHER_VIEW')")
```

Service gate:

```java
accessScopeService.assertSystemAccess();
```

Lý do:
- Voucher không phải dữ liệu sở hữu của một branch cụ thể.
- Branch chỉ là phạm vi áp dụng voucher, không phải ownership scope.

---

## 12. Consistency Và Tác Động Với Cart/Order

Flow CRUD voucher phải tương thích với logic apply voucher trong `plan-03-cart-order-tdd.md`.

Expected behavior:
- Voucher mới tạo có thể được flow cart/order đọc ngay sau khi commit DB.
- Update branch scope làm thay đổi kết quả validate ở lần apply voucher tiếp theo.
- Disable voucher làm flow apply voucher reject ở request tiếp theo.
- Delete chỉ cho phép khi chưa có usage để tránh làm hỏng lịch sử order/voucher usage.

Không cần đổi schema vì:
- `vc_voucher` đã đủ dữ liệu rule.
- `vc_voucher_branch` đã đủ dữ liệu branch restriction.
- `vc_voucher_usage` đã đủ để kiểm tra historical usage.

---

## 13. Verification

Compile command:

```bash
./mvnw -q -DskipTests compile
```

Manual test cases:
- Tạo voucher `PERCENTAGE` hợp lệ -> success.
- Tạo voucher trùng `code` -> fail.
- Tạo voucher có `discountValue=120` với `PERCENTAGE` -> fail.
- Update voucher đổi branch scope -> GET detail phản ánh đúng `branchIds` mới.
- Disable voucher -> cart apply voucher bị reject ở request kế tiếp.
- Delete voucher chưa có usage -> success.
- Delete voucher đã có usage -> fail.
- GET `/api/v1/vouchers` với filter `status`, `discountType`, `branchId` -> trả đúng tập dữ liệu.

---

## 14. Next Steps

- Tạo `VoucherController`, `VoucherService`, request/response DTO cho CRUD.
- Bổ sung repository methods cho usage check, branch scope replace và list filtering.
- Thêm permission master `PERM_VOUCHER_*` vào module authorization.
- Thêm integration tests cho create/update/delete/list voucher.
- Đồng bộ FE backoffice form voucher với request/response contract của tài liệu này.
