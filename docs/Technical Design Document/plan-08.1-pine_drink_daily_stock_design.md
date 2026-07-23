# Pine Drink - Daily Variant Stock Design

## 1. Mục tiêu

Daily Variant Stock là cơ chế quản lý số lượng sản phẩm/biến thể có thể bán trong từng ngày theo từng chi nhánh.

Thay vì quản lý tồn kho nguyên liệu phức tạp như cà phê còn bao nhiêu gram, sữa còn bao nhiêu ml, hệ thống MVP của Pine Drink sẽ quản lý theo hướng đơn giản hơn:

```text
Một chi nhánh + một biến thể sản phẩm + một ngày
= số lượng ly có thể bán trong ngày đó.
```

Ví dụ:

```text
Chi nhánh Hà Nội
Ngày 2026-06-11
Cà phê sữa đá size M: 100 ly
Trà đào size L: 80 ly
```

Mục tiêu chính:

- Cho admin/staff set số lượng bán trong ngày.
- Cho client xem còn bao nhiêu ly có thể mua.
- Khi khách tạo đơn chờ thanh toán thì giữ số lượng bằng `reserved_quantity`.
- Khi thanh toán thành công thì chuyển từ reserved sang sold.
- Khi hủy hoặc timeout thanh toán thì trả reserved về available.
- Có log để debug và kiểm tra lịch sử thay đổi stock.

---

## 2. Lý do chọn Daily Variant Stock thay vì Inventory nguyên liệu

Ban đầu hệ thống có thể đi theo hướng inventory nguyên liệu:

```text
Product -> Recipe -> Ingredient -> Stock -> Stock Movement
```

Hướng đó thực tế hơn với chuỗi lớn như Highlands Coffee, nhưng khá nặng với Pine Drink ở giai đoạn MVP.

Vì vậy Pine Drink chọn hướng nhẹ hơn:

```text
Product Variant -> Daily Stock -> Reserve/Sold/Release
```

Ưu điểm:

- Dễ code hơn.
- Dễ demo hơn.
- Vẫn chống được oversell.
- Phù hợp với web đặt đồ uống MVP.
- Không biến project thành hệ thống ERP/inventory quá lớn.

Phần inventory nguyên liệu có thể để làm future improvement.

---

## 3. Khái niệm chính

### 3.1. daily_quantity

Số lượng tối đa có thể bán trong ngày.

Ví dụ:

```text
daily_quantity = 100
```

Nghĩa là hôm nay variant này được bán tối đa 100 ly.

### 3.2. sold_quantity

Số lượng đã bán thành công.

Ví dụ:

```text
sold_quantity = 20
```

Nghĩa là đã có 20 ly thanh toán thành công.

### 3.3. reserved_quantity

Số lượng đang được giữ cho các đơn đang chờ thanh toán.

Ví dụ:

```text
reserved_quantity = 5
```

Nghĩa là đang có 5 ly nằm trong các đơn `PENDING_PAYMENT`.

### 3.4. available_quantity

Số lượng còn có thể bán cho khách mới.

Công thức:

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

Client sẽ hiển thị:

```text
Còn 75 ly
```

---

## 4. Database Schema

### 4.1. Bảng `ce_branch_variant_daily_stock`

Bảng này lưu số lượng bán trong ngày của từng variant theo từng chi nhánh.

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

    CONSTRAINT fk_ce_branch_variant_daily_stock_branch
        FOREIGN KEY (branch_id) REFERENCES ce_branch(id),

    CONSTRAINT fk_ce_branch_variant_daily_stock_variant
        FOREIGN KEY (variant_id) REFERENCES pr_product_variant(id),

    INDEX idx_ce_branch_variant_daily_stock_branch_date (branch_id, stock_date),
    INDEX idx_ce_branch_variant_daily_stock_variant_date (variant_id, stock_date),
    INDEX idx_ce_branch_variant_daily_stock_date_status (stock_date, status),

    CHECK (daily_quantity >= 0),
    CHECK (sold_quantity >= 0),
    CHECK (reserved_quantity >= 0),
    CHECK (sold_quantity + reserved_quantity <= daily_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

Business key:

```text
branch_id + variant_id + stock_date
```

Nghĩa là trong cùng một ngày, một chi nhánh chỉ có một dòng stock cho một variant.

---

### 4.2. Bảng `ce_branch_variant_stock_log`

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

    CONSTRAINT fk_ce_branch_variant_stock_log_daily_stock
        FOREIGN KEY (daily_stock_id) REFERENCES ce_branch_variant_daily_stock(id),

    CONSTRAINT fk_ce_branch_variant_stock_log_order
        FOREIGN KEY (order_id) REFERENCES od_order(id) ON DELETE SET NULL,

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

Log giúp trả lời các câu hỏi:

- Ai set quota hôm nay?
- Vì sao daily_quantity thay đổi?
- Đơn nào đã reserve số lượng?
- Đơn nào thanh toán thành công?
- Đơn nào hủy/timeout và release stock?

---

## 5. Stock Log Action Type

Enum gợi ý:

```java
public enum BranchVariantStockActionType {
    SET_QUOTA,   // Set số lượng bán trong ngày
    COPY_QUOTA,  // Copy quota từ ngày khác
    ADJUST,      // Admin chỉnh quota trong ngày
    RESERVE,     // Giữ số lượng khi tạo order chờ thanh toán
    SOLD,        // Thanh toán thành công, chuyển reserved sang sold
    RELEASE      // Hủy/timeout, trả reserved về available
}
```

Ý nghĩa:

| Action | Ý nghĩa |
|---|---|
| SET_QUOTA | Admin set quota thủ công |
| COPY_QUOTA | Copy quota từ lịch sử |
| ADJUST | Chỉnh quota trong ngày |
| RESERVE | Giữ hàng cho đơn chờ thanh toán |
| SOLD | Đơn thanh toán thành công |
| RELEASE | Hủy/timeout, trả lại số lượng đã giữ |

---

## 6. Flow nghiệp vụ chính

### 6.1. Flow set quota đầu ngày

Admin/staff set số lượng có thể bán trong ngày.

Ví dụ:

```text
Cà phê sữa đá size M: 100 ly
```

Hệ thống xử lý:

```text
1. Tìm daily stock theo branch_id + variant_id + stock_date.
2. Nếu chưa có thì tạo mới.
3. Nếu đã có thì update daily_quantity.
4. sold_quantity = 0 nếu là dòng mới.
5. reserved_quantity = 0 nếu là dòng mới.
6. Ghi log SET_QUOTA.
```

Ví dụ DB sau khi set:

```text
daily_quantity = 100
sold_quantity = 0
reserved_quantity = 0
available = 100
```

---

### 6.2. Flow client xem sản phẩm

Khi client gọi danh sách sản phẩm, backend trả thêm stock info cho từng variant.

Công thức:

```text
available = daily_quantity - sold_quantity - reserved_quantity
```

Response variant gợi ý:

```json
{
  "id": "variant-id",
  "name": "Size M",
  "price": 35000,
  "availableQuantity": 75,
  "stockStatus": "AVAILABLE"
}
```

Stock status gợi ý:

```text
available > 5  -> AVAILABLE
available 1-5  -> LOW_STOCK
available = 0  -> OUT_OF_STOCK
không có stock row -> NOT_SET hoặc OUT_OF_STOCK
```

Với MVP nên coi `NOT_SET` là không bán được để an toàn.

---

### 6.3. Flow thêm vào giỏ hàng

Khi khách thêm sản phẩm vào giỏ hàng:

```text
Không reserve stock.
```

Lý do:

- Giỏ hàng chưa chắc mua.
- Nếu reserve ở cart thì user có thể giữ hàng quá lâu.
- Dễ gây hết hàng giả.

Chỉ nên check nhẹ:

```text
Nếu available > 0 thì cho thêm vào giỏ.
Nếu available = 0 thì disable nút mua.
```

---

### 6.4. Flow tạo order chờ thanh toán

Khi khách bấm đặt hàng:

```http
POST /api/v1/orders
```

Hệ thống xử lý:

```text
1. Validate branch, variant, quantity.
2. Tạo order status = PENDING_PAYMENT.
3. Với từng order item, tìm daily stock hôm nay.
4. Lock stock row để chống race condition.
5. Tính available = daily - sold - reserved.
6. Nếu không đủ thì throw INSUFFICIENT_STOCK.
7. Nếu đủ thì reserved_quantity += quantity.
8. Ghi log RESERVE.
9. Trả order/payment info.
```

Ví dụ trước khi reserve:

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5
available = 75
```

Khách đặt 2 ly:

```text
reserved_quantity = 7
sold_quantity = 20
available = 73
```

Log:

```text
action_type = RESERVE
quantity = 2
before_reserved_quantity = 5
after_reserved_quantity = 7
order_id = orderId
reason = Reserve stock for pending payment order
```

---

### 6.5. Flow thanh toán thành công

Khi payment callback/webhook báo thành công:

```text
1. Lấy order.
2. Chỉ xử lý nếu order đang PENDING_PAYMENT.
3. Với từng order item:
   - reserved_quantity -= quantity
   - sold_quantity += quantity
4. Update order status = PAID hoặc CONFIRMED.
5. Ghi log SOLD.
```

Ví dụ trước payment success:

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 7
available = 73
```

Đơn thanh toán thành công 2 ly:

```text
daily_quantity = 100
sold_quantity = 22
reserved_quantity = 5
available = 73
```

Lưu ý:

```text
available không đổi vì 2 ly đã được giữ từ trước.
```

Log:

```text
action_type = SOLD
quantity = 2
before_sold_quantity = 20
after_sold_quantity = 22
before_reserved_quantity = 7
after_reserved_quantity = 5
order_id = orderId
reason = Payment success, convert reserved stock to sold
```

---

### 6.6. Flow hủy order

Nếu khách hủy đơn khi order đang `PENDING_PAYMENT`:

```text
1. Lấy order.
2. Kiểm tra order đang PENDING_PAYMENT.
3. Với từng order item:
   - reserved_quantity -= quantity
4. Update order status = CANCELLED.
5. Ghi log RELEASE.
```

Ví dụ trước khi hủy:

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 7
available = 73
```

Hủy đơn 2 ly:

```text
daily_quantity = 100
sold_quantity = 20
reserved_quantity = 5
available = 75
```

Log:

```text
action_type = RELEASE
quantity = 2
before_reserved_quantity = 7
after_reserved_quantity = 5
order_id = orderId
reason = Order cancelled, release reserved stock
```

---

### 6.7. Flow timeout thanh toán

Khi tạo order, hệ thống nên set thời gian hết hạn thanh toán.

Ví dụ:

```text
payment_expired_at = now + 10 minutes
```

Scheduled job chạy mỗi phút:

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

Log:

```text
action_type = RELEASE
quantity = 2
order_id = orderId
reason = Payment timeout, release reserved stock
```

---

### 6.8. Flow admin chỉnh quota trong ngày

Ví dụ hôm nay set ban đầu 100 ly, sau đó staff pha thêm nên tăng lên 120 ly.

Hệ thống xử lý:

```text
1. Lock daily stock row.
2. Lưu before_daily_quantity.
3. Update daily_quantity.
4. Ghi log ADJUST.
```

Nếu giảm quota, phải đảm bảo:

```text
new_daily_quantity >= sold_quantity + reserved_quantity
```

Ví dụ:

```text
sold_quantity = 60
reserved_quantity = 10
minimum daily_quantity = 70
```

Không được set daily_quantity xuống 50.

---

## 7. Copy quota từ lịch sử

Nếu ngày nào admin cũng phải set quota từng variant thì rất mất công.

Vì vậy nên có giải pháp:

```text
Copy quota từ một ngày trong quá khứ sang ngày mới.
```

Ví dụ:

```text
Copy quota từ hôm qua sang hôm nay.
Copy quota từ cùng thứ tuần trước sang hôm nay.
```

### 7.1. Nguyên tắc copy

Chỉ copy:

```text
daily_quantity
```

Không copy:

```text
sold_quantity
reserved_quantity
```

Vì ngày mới phải bắt đầu từ 0.

Ví dụ source date:

```text
2026-06-10
variant A:
daily_quantity = 100
sold_quantity = 80
reserved_quantity = 0
```

Target date:

```text
2026-06-11
variant A:
daily_quantity = 100
sold_quantity = 0
reserved_quantity = 0
```

---

### 7.2. API copy quota

```http
POST /api/v1/admin/daily-stocks/copy
```

Request:

```json
{
  "branchId": "branch-id",
  "sourceDate": "2026-06-10",
  "targetDate": "2026-06-11",
  "overwrite": false,
  "reason": "Copy quota from previous day"
}
```

Response:

```json
{
  "sourceDate": "2026-06-10",
  "targetDate": "2026-06-11",
  "createdCount": 20,
  "updatedCount": 0,
  "skippedCount": 3
}
```

Ý nghĩa `overwrite`:

```text
false: nếu targetDate đã có stock thì bỏ qua.
true: nếu targetDate đã có stock thì update daily_quantity theo sourceDate.
```

Mặc định nên dùng:

```text
overwrite = false
```

để an toàn.

---

### 7.3. Rule khi overwrite

Nếu targetDate đã có sold hoặc reserved thì không được overwrite bừa.

Rule:

```text
newDailyQuantity >= soldQuantity + reservedQuantity
```

Nếu không thỏa thì báo lỗi hoặc skip dòng đó.

---

### 7.4. Log khi copy quota

Khi copy quota, ghi log:

```text
action_type = COPY_QUOTA
quantity = copied daily_quantity
before_daily_quantity = old target daily quantity
after_daily_quantity = copied daily quantity
reason = Copy from 2026-06-10
```

---

## 8. API cần làm

### 8.1. Admin Daily Stock API

#### Set quota

```http
POST /api/v1/admin/daily-stocks
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

#### Update quota

```http
PATCH /api/v1/admin/daily-stocks/{id}/quota
```

Request:

```json
{
  "dailyQuantity": 120,
  "reason": "Pha thêm nguyên liệu nên tăng quota"
}
```

#### Get daily stocks by branch/date

```http
GET /api/v1/admin/daily-stocks?branchId={branchId}&stockDate=2026-06-11
```

#### Get stock logs

```http
GET /api/v1/admin/daily-stocks/{id}/logs
```

#### Copy quota from history

```http
POST /api/v1/admin/daily-stocks/copy
```

---

### 8.2. Client Product API

Product API nên bổ sung stock info cho variant.

```http
GET /api/v1/products?branchId={branchId}
```

Response gợi ý:

```json
{
  "id": "product-id",
  "name": "Cà phê sữa đá",
  "variants": [
    {
      "id": "variant-m-id",
      "name": "Size M",
      "price": 35000,
      "availableQuantity": 75,
      "stockStatus": "AVAILABLE"
    },
    {
      "id": "variant-l-id",
      "name": "Size L",
      "price": 42000,
      "availableQuantity": 0,
      "stockStatus": "OUT_OF_STOCK"
    }
  ]
}
```

Lưu ý:

```text
Product API chỉ đọc stock để hiển thị.
Không xử lý reserve/sold/release trong ProductService.
```

---

### 8.3. Order API

Reserve stock không nên là API public riêng. Nó nên xảy ra bên trong create order.

```http
POST /api/v1/orders
```

Flow:

```text
create order -> reserve stock -> order PENDING_PAYMENT
```

Cancel order:

```http
PATCH /api/v1/orders/{orderId}/cancel
```

Flow:

```text
cancel order -> release stock -> order CANCELLED
```

---

### 8.4. Payment API

Payment callback/webhook xử lý confirm sold.

```http
POST /api/v1/payments/vnpay/callback
```

Flow:

```text
payment success -> reserved -= qty -> sold += qty -> order PAID/CONFIRMED
```

---

## 9. Service Design

Service chính:

```java
BranchVariantDailyStockService
```

Method gợi ý:

```java
DailyStockResponse setQuota(SetDailyStockRequest request, String actorId);

DailyStockResponse updateQuota(String stockId, UpdateDailyStockQuotaRequest request, String actorId);

PageResponse<DailyStockResponse> getDailyStocks(String branchId, LocalDate stockDate, Pageable pageable);

List<DailyStockLogResponse> getLogs(String dailyStockId);

CopyDailyStockResult copyQuota(CopyDailyStockRequest request, String actorId);

void reserveForOrder(Order order);

void confirmSoldForOrder(Order order);

void releaseForOrder(Order order);

Integer getAvailableQuantity(String branchId, String variantId, LocalDate stockDate);
```

---

## 10. Repository Lock chống oversell

Case nguy hiểm:

```text
Còn 1 ly.
2 khách cùng đặt 1 ly.
Nếu không lock, cả 2 có thể cùng đặt thành công.
```

Repository nên có query lock:

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

Flow khi reserve:

```text
Customer A reserve
-> lock row
-> available = 1
-> reserved += 1
-> commit

Customer B reserve
-> chờ lock
-> sau khi A commit, available = 0
-> báo hết hàng
```

---

## 11. Pseudo-code nghiệp vụ

### 11.1. Reserve

```java
@Transactional
public void reserve(String branchId, String variantId, LocalDate date, int quantity, String orderId, String actorId) {
    BranchVariantDailyStock stock = repository.findForUpdate(branchId, variantId, date)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_SET));

    int available = stock.getDailyQuantity()
            - stock.getSoldQuantity()
            - stock.getReservedQuantity();

    if (available < quantity) {
        throw new BaseException(ErrorCode.INSUFFICIENT_STOCK);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() + quantity);

    repository.save(stock);

    stockLogService.createLog(stock, orderId, RESERVE, quantity, before, StockSnapshot.from(stock), actorId,
            "Reserve stock for pending payment order");
}
```

### 11.2. Confirm sold

```java
@Transactional
public void confirmSold(String branchId, String variantId, LocalDate date, int quantity, String orderId, String actorId) {
    BranchVariantDailyStock stock = repository.findForUpdate(branchId, variantId, date)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_SET));

    if (stock.getReservedQuantity() < quantity) {
        throw new BaseException(ErrorCode.INVALID_RESERVED_STOCK);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() - quantity);
    stock.setSoldQuantity(stock.getSoldQuantity() + quantity);

    repository.save(stock);

    stockLogService.createLog(stock, orderId, SOLD, quantity, before, StockSnapshot.from(stock), actorId,
            "Payment success, convert reserved stock to sold");
}
```

### 11.3. Release

```java
@Transactional
public void release(String branchId, String variantId, LocalDate date, int quantity, String orderId, String actorId) {
    BranchVariantDailyStock stock = repository.findForUpdate(branchId, variantId, date)
            .orElseThrow(() -> new BaseException(ErrorCode.STOCK_NOT_SET));

    if (stock.getReservedQuantity() < quantity) {
        throw new BaseException(ErrorCode.INVALID_RESERVED_STOCK);
    }

    StockSnapshot before = StockSnapshot.from(stock);

    stock.setReservedQuantity(stock.getReservedQuantity() - quantity);

    repository.save(stock);

    stockLogService.createLog(stock, orderId, RELEASE, quantity, before, StockSnapshot.from(stock), actorId,
            "Release reserved stock");
}
```

---

## 12. Order status mapping với stock

| Order Status | Stock Meaning |
|---|---|
| PENDING_PAYMENT | Đã reserve stock |
| PAID / CONFIRMED | Đã chuyển reserved sang sold |
| CANCELLED | Đã release reserved |
| EXPIRED | Đã release reserved |

Lưu ý quan trọng:

```text
Không được xử lý stock nhiều lần cho cùng một order.
```

Ví dụ payment callback gọi lại lần 2:

```text
Nếu order không còn PENDING_PAYMENT thì bỏ qua, không SOLD lần nữa.
```

---

## 13. Luồng tổng quát

```text
Admin set quota đầu ngày
-> ce_branch_variant_daily_stock.daily_quantity = input
-> log SET_QUOTA

Client xem sản phẩm
-> tính available = daily - sold - reserved
-> hiển thị còn bao nhiêu ly

Customer tạo order
-> reserve stock
-> order = PENDING_PAYMENT
-> log RESERVE

Payment success
-> reserved giảm
-> sold tăng
-> order = PAID / CONFIRMED
-> log SOLD

Customer cancel / payment timeout
-> reserved giảm
-> order = CANCELLED / EXPIRED
-> log RELEASE

Admin chỉnh quota
-> daily_quantity thay đổi
-> log ADJUST

Admin copy quota từ lịch sử
-> tạo quota ngày mới từ sourceDate
-> sold/reserved = 0
-> log COPY_QUOTA
```

---

## 14. Future Improvement

Sau MVP, có thể mở rộng:

1. Tự động gợi ý quota theo lịch sử bán hàng.
2. Copy quota theo cùng thứ tuần trước.
3. Scheduled job tự tạo quota mỗi ngày.
4. Cảnh báo variant sắp hết hàng.
5. Kết hợp recipe/ingredient inventory để tự tính số ly có thể bán dựa trên nguyên liệu thật.
6. Dashboard thống kê: daily quota, sold, reserved, sell-through rate.

Ví dụ gợi ý quota sau này:

```text
suggestedQuantity = average(soldQuantity của 3 ngày cùng thứ gần nhất) + buffer 10%
```

---

## 15. Câu mô tả ngắn gọn khi phỏng vấn

Trong Pine Drink, em chọn cơ chế Daily Variant Stock để quản lý số lượng sản phẩm có thể bán theo ngày và theo chi nhánh. Mỗi variant sẽ có `daily_quantity`, `reserved_quantity` và `sold_quantity`. Khi khách tạo đơn chờ thanh toán, hệ thống kiểm tra số lượng khả dụng theo công thức `daily - reserved - sold`, nếu đủ thì reserve số lượng đó. Khi thanh toán thành công, hệ thống chuyển reserved sang sold. Nếu khách hủy hoặc hết hạn thanh toán thì release reserved để người khác có thể mua. Cách này giúp tránh oversell nhưng vẫn giữ nghiệp vụ vừa sức cho giai đoạn MVP.

---

## 16. Kết luận

Daily Variant Stock là giải pháp phù hợp cho Pine Drink MVP vì nó cân bằng giữa tính thực tế và độ phức tạp.

Thiết kế này có các điểm mạnh:

- Dễ hiểu.
- Dễ code.
- Có chống oversell bằng reserve.
- Có log để audit/debug.
- Có thể mở rộng copy quota từ lịch sử.
- Không cần triển khai inventory nguyên liệu quá nặng ngay từ đầu.

Công thức cốt lõi cần nhớ:

```text
available_quantity = daily_quantity - sold_quantity - reserved_quantity
```

Và 3 thao tác quan trọng nhất:

```text
reserve: reserved_quantity += qty
payment success: reserved_quantity -= qty, sold_quantity += qty
cancel/timeout: reserved_quantity -= qty
```
