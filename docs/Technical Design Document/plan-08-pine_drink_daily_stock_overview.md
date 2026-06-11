# Pine Drink - Daily Variant Stock Overview

## 1. Mục tiêu thay đổi

Ban đầu dự án có hướng quản lý tồn kho theo nguyên liệu với các bảng `iv_*`, gồm nguyên liệu, công thức, tồn kho nguyên liệu và lịch sử kho. Hướng đó khá thực tế nhưng quá nặng cho phạm vi MVP của Pine Drink.

Vì vậy, hệ thống chuyển sang cơ chế nhẹ hơn:

> Mỗi chi nhánh cấu hình số lượng có thể bán trong ngày cho từng biến thể sản phẩm. Khi khách đặt hàng, hệ thống giữ số lượng đó trong lúc chờ thanh toán. Nếu thanh toán thành công thì chuyển sang đã bán. Nếu hủy hoặc hết hạn thanh toán thì trả lại số lượng đã giữ.

Cơ chế này giúp:

- Người dùng thấy được sản phẩm còn bao nhiêu ly trong ngày.
- Tránh bán quá số lượng đã cấu hình.
- Dễ code hơn inventory nguyên liệu.
- Vẫn có nghiệp vụ reserve để chống oversell.
- Có log để kiểm tra lịch sử thay đổi số lượng.

---

## 2. Khái niệm chính

### 2.1 Daily stock

Daily stock là số lượng sản phẩm/variant mà một chi nhánh có thể bán trong một ngày cụ thể.

Ví dụ:

```text
Chi nhánh A
Ngày: 2026-06-11
Cà phê sữa đá size M: 100 ly
Trà đào size L: 80 ly
```

Mỗi dòng daily stock được xác định bởi:

```text
branch_id + variant_id + stock_date
```

---

### 2.2 Daily quantity

`daily_quantity` là tổng số lượng có thể bán trong ngày.

Ví dụ:

```text
daily_quantity = 100
```

Nghĩa là hôm nay variant này được phép bán tối đa 100 ly tại chi nhánh đó.

---

### 2.3 Reserved quantity

`reserved_quantity` là số lượng đang được giữ cho các đơn hàng đang chờ thanh toán.

Ví dụ:

```text
reserved_quantity = 5
```

Nghĩa là đang có 5 ly được giữ trong các đơn `PENDING_PAYMENT`.

---

### 2.4 Sold quantity

`sold_quantity` là số lượng đã bán thành công.

Ví dụ:

```text
sold_quantity = 20
```

Nghĩa là đã có 20 ly được thanh toán thành công.

---

### 2.5 Available quantity

Số lượng còn có thể bán được tính theo công thức:

```text
available_quantity = daily_quantity - sold_quantity - reserved_quantity
```

Ví dụ:

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5

available_quantity = 100 - 20 - 5 = 75
```

Người dùng sẽ thấy sản phẩm còn 75 ly.

---

## 3. Database schema

### 3.1 Bảng `ce_branch_variant_daily_stock`

Bảng này lưu số lượng bán trong ngày theo chi nhánh và variant.

```sql
CREATE TABLE ce_branch_variant_daily_stock (
    id CHAR(36) NOT NULL PRIMARY KEY,
    branch_id CHAR(36) NOT NULL,
    variant_id CHAR(36) NOT NULL,
    stock_date DATE NOT NULL,
    daily_quantity INT NOT NULL DEFAULT 0,
    sold_quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    updated_by CHAR(36) NULL,
    UNIQUE KEY uk_ce_branch_variant_stock_date (branch_id, variant_id, stock_date),
    CONSTRAINT fk_ce_branch_variant_daily_stock_branch FOREIGN KEY (branch_id) REFERENCES ce_branch(id),
    CONSTRAINT fk_ce_branch_variant_daily_stock_variant FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id),
    INDEX idx_ce_branch_variant_daily_stock_branch_date (branch_id, stock_date),
    INDEX idx_ce_branch_variant_daily_stock_variant_date (variant_id, stock_date),
    CHECK (daily_quantity >= 0),
    CHECK (sold_quantity >= 0),
    CHECK (reserved_quantity >= 0),
    CHECK (sold_quantity + reserved_quantity <= daily_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Ý nghĩa các field chính:

| Field | Ý nghĩa |
|---|---|
| `branch_id` | Chi nhánh áp dụng stock |
| `variant_id` | Biến thể sản phẩm, ví dụ size M, size L |
| `stock_date` | Ngày áp dụng số lượng bán |
| `daily_quantity` | Tổng số lượng có thể bán trong ngày |
| `sold_quantity` | Số lượng đã bán thành công |
| `reserved_quantity` | Số lượng đang giữ cho đơn chờ thanh toán |
| `status` | Trạng thái dòng stock |

Ràng buộc quan trọng:

```sql
CHECK (sold_quantity + reserved_quantity <= daily_quantity)
```

Ràng buộc này giúp database không cho tồn tại trạng thái sai, ví dụ đã bán và đang giữ nhiều hơn số lượng có thể bán.

---

### 3.2 Bảng `ce_branch_variant_stock_log`

Bảng này lưu lịch sử thay đổi stock.

```sql
CREATE TABLE ce_branch_variant_stock_log (
    id CHAR(36) NOT NULL PRIMARY KEY,
    daily_stock_id CHAR(36) NOT NULL,
    order_id CHAR(36) NULL,
    action_type VARCHAR(40) NOT NULL,
    quantity INT NOT NULL,
    before_daily_quantity INT NOT NULL,
    after_daily_quantity INT NOT NULL,
    before_sold_quantity INT NOT NULL,
    after_sold_quantity INT NOT NULL,
    before_reserved_quantity INT NOT NULL,
    after_reserved_quantity INT NOT NULL,
    reason VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by CHAR(36) NULL,
    CONSTRAINT fk_ce_branch_variant_stock_log_daily_stock FOREIGN KEY (daily_stock_id) REFERENCES ce_branch_variant_daily_stock(id),
    CONSTRAINT fk_ce_branch_variant_stock_log_order FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL,
    INDEX idx_ce_branch_variant_stock_log_stock_created (daily_stock_id, created_at),
    INDEX idx_ce_branch_variant_stock_log_order (order_id),
    INDEX idx_ce_branch_variant_stock_log_action_created (action_type, created_at),
    CHECK (quantity > 0),
    CHECK (before_daily_quantity >= 0),
    CHECK (after_daily_quantity >= 0),
    CHECK (before_sold_quantity >= 0),
    CHECK (after_sold_quantity >= 0),
    CHECK (before_reserved_quantity >= 0),
    CHECK (after_reserved_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Ý nghĩa:

| Field | Ý nghĩa |
|---|---|
| `daily_stock_id` | Dòng daily stock bị thay đổi |
| `order_id` | Đơn hàng liên quan, nếu có |
| `action_type` | Loại hành động: set quota, reserve, sold, release, adjust |
| `quantity` | Số lượng tác động |
| `before_*` | Giá trị trước khi thay đổi |
| `after_*` | Giá trị sau khi thay đổi |
| `reason` | Lý do thay đổi |
| `created_by` | Người thao tác |

---

## 4. Action type

Nên dùng enum:

```java
public enum BranchVariantStockActionType {
    SET_QUOTA,  // Set số lượng bán trong ngày
    ADJUST,     // Admin/staff chỉnh số lượng trong ngày
    RESERVE,    // Giữ số lượng cho đơn chờ thanh toán
    SOLD,       // Thanh toán thành công, chuyển reserved sang sold
    RELEASE     // Hủy hoặc timeout, trả reserved về available
}
```

Ý nghĩa từng action:

| Action | Ý nghĩa |
|---|---|
| `SET_QUOTA` | Tạo hoặc set số lượng bán trong ngày |
| `ADJUST` | Điều chỉnh quota trong ngày |
| `RESERVE` | Giữ hàng khi khách tạo đơn chờ thanh toán |
| `SOLD` | Chuyển số lượng từ reserved sang sold khi thanh toán thành công |
| `RELEASE` | Trả lại số lượng đã giữ khi đơn hủy hoặc hết hạn thanh toán |

---

## 5. Flow nghiệp vụ tổng quát

```text
Admin set quota đầu ngày
→ ce_branch_variant_daily_stock.daily_quantity = input
→ log SET_QUOTA

Client xem sản phẩm
→ tính available = daily - sold - reserved
→ hiển thị còn bao nhiêu ly

Customer tạo order
→ reserve stock
→ order = PENDING_PAYMENT
→ log RESERVE

Payment success
→ reserved giảm
→ sold tăng
→ order = PAID / CONFIRMED
→ log SOLD

Customer cancel / payment timeout
→ reserved giảm
→ order = CANCELLED / EXPIRED
→ log RELEASE

Admin chỉnh quota
→ daily_quantity thay đổi
→ log ADJUST
```

---

## 6. Flow 1: Set quota đầu ngày

### Input

Ví dụ admin/staff set:

```text
Chi nhánh: Branch A
Ngày: 2026-06-11
Variant: Cà phê sữa đá size M
Số lượng bán: 100 ly
```

### Xử lý

```text
1. Tìm daily stock theo branch_id + variant_id + stock_date.
2. Nếu chưa có thì tạo mới.
3. Nếu đã có thì cập nhật daily_quantity.
4. Ghi log SET_QUOTA hoặc ADJUST.
```

### DB sau khi set

```text
daily_quantity = 100
sold_quantity = 0
reserved_quantity = 0
available = 100
```

### Log

```text
action_type = SET_QUOTA
quantity = 100
before_daily_quantity = 0
after_daily_quantity = 100
before_sold_quantity = 0
after_sold_quantity = 0
before_reserved_quantity = 0
after_reserved_quantity = 0
reason = Set quota đầu ngày
```

---

## 7. Flow 2: Client xem sản phẩm

Khi client gọi danh sách sản phẩm, backend cần lấy stock hôm nay theo branch và variant.

### Công thức

```text
available_quantity = daily_quantity - sold_quantity - reserved_quantity
```

### Ví dụ

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5
available_quantity = 75
```

Response có thể thêm:

```json
{
  "variantId": "variant-id",
  "variantName": "Size M",
  "availableQuantity": 75,
  "stockStatus": "AVAILABLE"
}
```

Nếu `availableQuantity = 0`:

```json
{
  "availableQuantity": 0,
  "stockStatus": "OUT_OF_STOCK"
}
```

---

## 8. Flow 3: Khách tạo order và reserve stock

### Không reserve ở giỏ hàng

Khi khách thêm sản phẩm vào giỏ, không nên reserve stock.

Lý do: giỏ hàng chưa chắc thanh toán, nếu reserve từ giỏ thì stock dễ bị giữ ảo.

Chỉ reserve khi khách bấm đặt hàng hoặc chuyển sang bước thanh toán.

---

### Khi tạo order

Ví dụ khách đặt:

```text
Cà phê sữa đá size M x 2
```

Backend xử lý:

```text
1. Validate branch, product, variant.
2. Lấy daily stock hôm nay theo branch_id + variant_id + stock_date.
3. Lock dòng daily stock để tránh race condition.
4. Tính available = daily - sold - reserved.
5. Nếu available < quantity thì báo không đủ stock.
6. Nếu đủ thì reserved_quantity += quantity.
7. Tạo order status = PENDING_PAYMENT.
8. Ghi log RESERVE.
```

### Ví dụ trước reserve

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5
available = 75
```

Khách đặt 2 ly.

### Sau reserve

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 7
available = 73
```

### Log

```text
action_type = RESERVE
quantity = 2
before_daily_quantity = 100
after_daily_quantity = 100
before_sold_quantity = 20
after_sold_quantity = 20
before_reserved_quantity = 5
after_reserved_quantity = 7
order_id = orderId
reason = Reserve stock for pending payment order
```

---

## 9. Flow 4: Payment success

Khi thanh toán thành công, số lượng đã giữ sẽ chuyển thành đã bán.

### Xử lý

```text
1. Payment callback/webhook báo thành công.
2. Lấy order.
3. Chỉ xử lý nếu order đang PENDING_PAYMENT.
4. Với từng order item:
   - reserved_quantity -= quantity
   - sold_quantity += quantity
5. Update order status = PAID hoặc CONFIRMED.
6. Ghi log SOLD.
7. Publish notification/email event nếu cần.
```

### Ví dụ trước payment success

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 7
available = 73
```

Đơn thanh toán thành công 2 ly.

### Sau payment success

```text
daily_quantity = 100
sold_quantity = 22
reserved_quantity = 5
available = 73
```

Available không đổi vì 2 ly đó đã bị giữ từ trước. Chỉ chuyển trạng thái từ reserved sang sold.

### Log

```text
action_type = SOLD
quantity = 2
before_daily_quantity = 100
after_daily_quantity = 100
before_sold_quantity = 20
after_sold_quantity = 22
before_reserved_quantity = 7
after_reserved_quantity = 5
order_id = orderId
reason = Payment success, convert reserved stock to sold
```

---

## 10. Flow 5: Cancel hoặc payment timeout

Nếu khách hủy đơn hoặc hết hạn thanh toán, số lượng đã giữ phải được trả lại.

### Xử lý

```text
1. Lấy order.
2. Chỉ xử lý nếu order đang PENDING_PAYMENT.
3. Với từng order item:
   - reserved_quantity -= quantity
4. Update order status = CANCELLED hoặc EXPIRED.
5. Ghi log RELEASE.
```

### Ví dụ trước release

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 7
available = 73
```

Đơn bị hủy 2 ly.

### Sau release

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5
available = 75
```

### Log

```text
action_type = RELEASE
quantity = 2
before_daily_quantity = 100
after_daily_quantity = 100
before_sold_quantity = 20
after_sold_quantity = 20
before_reserved_quantity = 7
after_reserved_quantity = 5
order_id = orderId
reason = Order cancelled or payment timeout, release reserved stock
```

---

## 11. Flow 6: Admin điều chỉnh quota trong ngày

Ví dụ ban đầu set 100 ly, sau đó staff muốn tăng lên 120 ly.

### Xử lý

```text
1. Lock dòng daily stock.
2. Kiểm tra số lượng mới hợp lệ.
3. daily_quantity = newQuantity.
4. Ghi log ADJUST.
```

### Điều kiện quan trọng

Không được set `daily_quantity` nhỏ hơn:

```text
sold_quantity + reserved_quantity
```

Ví dụ:

```text
sold_quantity = 60
reserved_quantity = 10
minimum daily_quantity = 70
```

Nếu admin set xuống 50 thì phải báo lỗi.

### Log tăng quota

```text
action_type = ADJUST
quantity = 20
before_daily_quantity = 100
after_daily_quantity = 120
reason = Tăng quota do chuẩn bị thêm hàng
```

### Log giảm quota

Nếu giảm từ 100 xuống 80:

```text
action_type = ADJUST
quantity = 20
before_daily_quantity = 100
after_daily_quantity = 80
reason = Giảm quota do thiếu hàng
```

`quantity` vẫn là số dương. Muốn biết tăng hay giảm thì nhìn `before_daily_quantity` và `after_daily_quantity`.

---

## 12. Chống race condition

### Vấn đề

Ví dụ còn 1 ly nhưng có 2 khách cùng đặt cùng lúc.

Nếu không lock:

```text
Customer A thấy còn 1 ly.
Customer B cũng thấy còn 1 ly.
Cả 2 cùng đặt thành công.
Kết quả: bán quá số lượng.
```

### Cách xử lý

Reserve stock phải chạy trong transaction và lock dòng daily stock.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    select s from BranchVariantDailyStock s
    where s.branch.id = :branchId
      and s.variant.id = :variantId
      and s.stockDate = :stockDate
""")
Optional<BranchVariantDailyStock> findForUpdate(
        String branchId,
        String variantId,
        LocalDate stockDate
);
```

Flow đúng:

```text
Customer A reserve
→ lock row
→ available = 1
→ reserved += 1
→ commit

Customer B reserve
→ chờ lock
→ sau khi A commit, available = 0
→ báo hết hàng
```

---

## 13. Service gợi ý

Nên có service:

```text
BranchVariantDailyStockService
```

Các method chính:

```java
void setQuota(String branchId, String variantId, LocalDate stockDate, int dailyQuantity);

void reserve(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);

void confirmSold(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);

void release(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId);

int getAvailableQuantity(String branchId, String variantId, LocalDate stockDate);
```

---

## 14. Order status liên quan đến stock

Đề xuất mapping:

| Order status | Stock behavior |
|---|---|
| `PENDING_PAYMENT` | Đã reserve stock |
| `PAID` / `CONFIRMED` | Chuyển reserved sang sold |
| `CANCELLED` | Release reserved nếu đơn chưa thanh toán |
| `EXPIRED` | Release reserved do hết hạn thanh toán |

Lưu ý quan trọng:

```text
Không được SOLD hoặc RELEASE nhiều lần cho cùng một order.
```

Payment callback có thể bị gọi lại nhiều lần, nên cần idempotent:

```text
Nếu order không còn PENDING_PAYMENT thì không xử lý stock lần nữa.
```

---

## 15. API gợi ý

### Admin set quota

```http
POST /api/v1/admin/branch-variant-daily-stocks
```

Request:

```json
{
  "branchId": "branch-id",
  "variantId": "variant-id",
  "stockDate": "2026-06-11",
  "dailyQuantity": 100,
  "reason": "Set quota đầu ngày"
}
```

---

### Admin update quota

```http
PATCH /api/v1/admin/branch-variant-daily-stocks/{id}/quota
```

Request:

```json
{
  "dailyQuantity": 120,
  "reason": "Tăng quota do chuẩn bị thêm hàng"
}
```

---

### Client lấy available stock

```http
GET /api/v1/products?branchId={branchId}
```

Response variant có thể kèm:

```json
{
  "variantId": "variant-id",
  "variantName": "Size M",
  "availableQuantity": 75,
  "stockStatus": "AVAILABLE"
}
```

---

### Admin xem log stock

```http
GET /api/v1/admin/branch-variant-stock-logs?dailyStockId={id}
```

Hoặc:

```http
GET /api/v1/admin/branch-variant-stock-logs?orderId={orderId}
```

---

## 16. Repository gợi ý

```java
public interface BranchVariantDailyStockRepository extends JpaRepository<BranchVariantDailyStock, String> {

    Optional<BranchVariantDailyStock> findByBranchIdAndVariantIdAndStockDate(
            String branchId,
            String variantId,
            LocalDate stockDate
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        select s from BranchVariantDailyStock s
        where s.branch.id = :branchId
          and s.variant.id = :variantId
          and s.stockDate = :stockDate
    """)
    Optional<BranchVariantDailyStock> findForUpdate(
            @Param("branchId") String branchId,
            @Param("variantId") String variantId,
            @Param("stockDate") LocalDate stockDate
    );
}
```

Log repository:

```java
public interface BranchVariantStockLogRepository extends JpaRepository<BranchVariantStockLog, String> {

    Page<BranchVariantStockLog> findByDailyStockId(String dailyStockId, Pageable pageable);

    Page<BranchVariantStockLog> findByOrderId(String orderId, Pageable pageable);
}
```

---

## 17. Pseudo-code service

### Reserve

```java
@Transactional
public void reserve(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
    BranchVariantDailyStock stock = stockRepository
            .findForUpdate(branchId, variantId, stockDate)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_FOUND));

    int available = stock.getDailyQuantity()
            - stock.getSoldQuantity()
            - stock.getReservedQuantity();

    if (available < quantity) {
        throw new BaseException(ErrorCode.INSUFFICIENT_STOCK);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

    StockSnapshot after = StockSnapshot.from(stock);

    stockRepository.save(stock);
    stockLogRepository.save(StockLog.reserve(stock, orderId, quantity, before, after));
}
```

---

### Confirm sold

```java
@Transactional
public void confirmSold(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
    BranchVariantDailyStock stock = stockRepository
            .findForUpdate(branchId, variantId, stockDate)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_FOUND));

    if (stock.getReservedQuantity() < quantity) {
        throw new BaseException(ErrorCode.INVALID_STOCK_RESERVATION);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
    stock.setSoldQuantity(stock.getSoldQuantity() + quantity);

    StockSnapshot after = StockSnapshot.from(stock);

    stockRepository.save(stock);
    stockLogRepository.save(StockLog.sold(stock, orderId, quantity, before, after));
}
```

---

### Release

```java
@Transactional
public void release(String branchId, String variantId, LocalDate stockDate, int quantity, String orderId) {
    BranchVariantDailyStock stock = stockRepository
            .findForUpdate(branchId, variantId, stockDate)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_FOUND));

    if (stock.getReservedQuantity() < quantity) {
        throw new BaseException(ErrorCode.INVALID_STOCK_RESERVATION);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() - quantity);

    StockSnapshot after = StockSnapshot.from(stock);

    stockRepository.save(stock);
    stockLogRepository.save(StockLog.release(stock, orderId, quantity, before, after));
}
```

---

## 18. Payment timeout

Khi tạo order chờ thanh toán, nên có field:

```text
payment_expired_at = now + 10 minutes
```

Có scheduled job chạy mỗi phút:

```text
Tìm order:
status = PENDING_PAYMENT
payment_expired_at < now
```

Với mỗi order hết hạn:

```text
1. Update order status = EXPIRED.
2. Release reserved stock.
3. Ghi log RELEASE.
```

RabbitMQ delayed message có thể dùng sau, nhưng MVP nên dùng scheduled job cho dễ.

---

## 19. Có dùng RabbitMQ trong flow stock không?

Không nên dùng RabbitMQ cho reserve/sold/release stock chính.

Lý do:

```text
Stock là nghiệp vụ cần chắc chắn ngay.
Nếu đẩy reserve qua queue, API có thể báo đặt hàng thành công nhưng consumer xử lý sau lại báo hết hàng.
```

Nên làm đồng bộ:

```text
OrderService tạo order
→ StockService reserve đồng bộ trong transaction
→ Thành công thì trả response
```

RabbitMQ nên dùng cho tác vụ phụ sau khi order thành công:

```text
Gửi notification
Gửi email
Ghi activity log phụ
Thông báo realtime cho staff
```

---

## 20. Những điểm cần nhớ

1. Không reserve khi add to cart.
2. Chỉ reserve khi tạo order/chờ thanh toán.
3. Payment success thì `reserved -= qty`, `sold += qty`.
4. Cancel/timeout thì `reserved -= qty`.
5. Admin không được set `daily_quantity < sold_quantity + reserved_quantity`.
6. Reserve phải lock row để chống oversell.
7. Log nên ghi mọi thay đổi quan trọng để dễ debug.
8. Payment callback phải idempotent, tránh trừ stock nhiều lần.
9. Không cần dùng inventory nguyên liệu trong MVP.
10. Daily stock là hướng vừa sức hơn cho Pine Drink.

---

## 21. Tóm tắt để trình bày

Trong Pine Drink, hệ thống quản lý số lượng bán theo ngày cho từng biến thể sản phẩm tại từng chi nhánh. Mỗi dòng stock gồm `daily_quantity`, `sold_quantity` và `reserved_quantity`. Khi khách xem sản phẩm, hệ thống tính số lượng còn lại bằng công thức `daily - sold - reserved`. Khi khách tạo đơn chờ thanh toán, hệ thống reserve số lượng cần mua để tránh người khác mua trùng. Nếu thanh toán thành công, số lượng reserved được chuyển sang sold. Nếu đơn bị hủy hoặc hết hạn thanh toán, số lượng reserved được release lại. Mọi thay đổi đều được ghi vào bảng stock log để kiểm tra lịch sử.

Cách này giúp dự án có nghiệp vụ tồn kho đủ thực tế, chống oversell, nhưng vẫn đơn giản hơn rất nhiều so với quản lý tồn kho nguyên liệu theo công thức pha chế.
