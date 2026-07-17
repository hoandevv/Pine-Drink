# Plan 8 - Daily Sellable Stock

## Muc tieu

Quan ly so luong san pham co the ban theo ngay cho tung chi nhanh. Day khong phai inventory nguyen lieu; khong co ingredient, recipe, hay stock movement theo nguyen lieu.

## Business Model

Staff/Admin set quota ban trong ngay:

```text
Ca phe sua da size M: 100 ly
Tra dao cam sa size L: 80 ly
Matcha latte size M: 50 ly
```

Client hien thi so luong con lai:

```text
available_quantity = daily_quantity - sold_quantity - reserved_quantity
```

## Database

### `ce_branch_variant_daily_stock`

Daily stock theo branch + product variant.

Key columns:

- `branch_id`: chi nhanh ban hang
- `variant_id`: variant san pham, da tro ve `pr_product_variant.product_id`
- `stock_date`: ngay ap dung quota
- `daily_quantity`: tong so ly co the ban trong ngay
- `sold_quantity`: so ly da thanh toan/hoan tat
- `reserved_quantity`: so ly dang giu cho order cho thanh toan

Rule:

- `UNIQUE(branch_id, variant_id, stock_date)`
- `daily_quantity >= 0`
- `sold_quantity >= 0`
- `reserved_quantity >= 0`
- `sold_quantity + reserved_quantity <= daily_quantity`

### `ce_branch_variant_stock_log`

Audit log cho moi lan thay doi stock.

Action types:

- `SET_QUOTA`
- `ADJUST_QUOTA`
- `RESERVE`
- `CONSUME`
- `RELEASE`

## Files Can Tao

| File | Mo ta |
| --- | --- |
| `entity/BranchVariantDailyStock.java` | Entity daily stock |
| `entity/BranchVariantStockLog.java` | Entity stock log |
| `repository/BranchVariantDailyStockRepository.java` | Repository daily stock |
| `repository/BranchVariantStockLogRepository.java` | Repository stock log |
| `service/DailyStockService.java` | Interface quan ly stock |
| `service/impl/DailyStockServiceImpl.java` | Implementation |
| `controller/DailyStockController.java` | REST API cho staff/admin |

## API Endpoints

| Method | Endpoint | Mo ta |
| --- | --- | --- |
| GET | `/daily-stocks?branchId=&date=` | Danh sach stock theo branch/date |
| GET | `/daily-stocks/available?branchId=&variantId=&date=` | Xem available cua variant |
| PUT | `/daily-stocks/quota` | Set/replace quota trong ngay |
| PATCH | `/daily-stocks/{id}/adjust` | Dieu chinh quota |
| GET | `/daily-stocks/{id}/logs?page=&size=` | Lich su thay doi stock |

## Order Flow

```text
Khach checkout
-> reserve stock
-> reserved_quantity += item quantity

Payment success / order confirmed
-> consume stock
-> reserved_quantity -= item quantity
-> sold_quantity += item quantity

Payment failed / cancel / timeout
-> release stock
-> reserved_quantity -= item quantity
```

## Atomic Update

Reserve phai atomic de tranh oversell:

```sql
UPDATE ce_branch_variant_daily_stock
SET reserved_quantity = reserved_quantity + :quantity
WHERE id = :id
  AND daily_quantity - sold_quantity - reserved_quantity >= :quantity;
```

Neu affected rows = 0 thi het hang hoac khong du so luong.

## Service Methods

```java
void setQuota(String branchId, String variantId, LocalDate date, int dailyQuantity);
void reserve(String branchId, String variantId, LocalDate date, int quantity, String orderId);
void consume(String branchId, String variantId, LocalDate date, int quantity, String orderId);
void release(String branchId, String variantId, LocalDate date, int quantity, String orderId);
int getAvailableQuantity(String branchId, String variantId, LocalDate date);
```

## Integration Rules

- Product availability van dung `mn_branch_product_availability`.
- Daily stock chi quan ly so luong ban theo ngay.
- `variant_id` la source of truth; khong duplicate `product_id` trong daily stock.
- Product khong co size nen co default variant.
- Khong co stock row thi API phai quy uoc ro: het hang hoac unlimited. MVP nen coi la het hang de staff bat buoc set quota.

## Checklist

- [x] Tao schema `ce_branch_variant_daily_stock`
- [x] Tao schema `ce_branch_variant_stock_log`
- [x] Tao entity + repository cho daily stock
- [x] Tao entity + repository cho stock log
- [ ] Tao DailyStockService
- [ ] Tao DailyStockController
- [ ] Implement set quota
- [ ] Implement atomic reserve
- [ ] Implement consume sau payment success
- [ ] Implement release khi cancel/timeout/payment failed
- [ ] Them available quantity vao menu response neu can
- [ ] Them pagination cho stock logs
