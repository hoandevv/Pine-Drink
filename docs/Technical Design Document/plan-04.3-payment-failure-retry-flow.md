# Payment Failure, Retry, and Change Method Flow

## Mục tiêu

Khi thanh toán online thất bại, đơn hàng không nên bị hủy ngay. Khách cần có lựa chọn:

- Thanh toán lại bằng cùng phương thức.
- Đổi sang phương thức khác như CASH/COD.
- Hủy đơn nếu không muốn tiếp tục.

## Nguyên tắc trạng thái

### Payment thất bại

Khi MoMo trả IPN thất bại hoặc backend phát hiện lỗi thanh toán:

```text
order.status = PENDING
order.paymentStatus = UNPAID
paymentTransaction.status = FAILED
paymentIntent.status = FAILED
```

Không chuyển:

```text
order.status = CANCELLED
```

Trừ khi:

- Khách chủ động hủy đơn.
- Đơn hết hạn thanh toán.
- Nhân viên/admin từ chối đơn.

### Payment thành công

Khi IPN MoMo hợp lệ và `resultCode = 0`:

```text
order.status = PENDING
order.paymentStatus = PAID
paymentTransaction.status = PAID
paymentIntent.status = PAID
```

`order.status` vẫn là trạng thái xử lý đơn. Không nên dùng `order.status` để biểu diễn thanh toán.

## Ý nghĩa từng trạng thái

### `order.status`

Dùng cho vòng đời đơn hàng:

```text
PENDING
CONFIRMED
PREPARING
READY
DELIVERING
COMPLETED
CANCELLED
REJECTED
```

### `order.paymentStatus`

Dùng cho trạng thái tiền:

```text
UNPAID
PAID
FAILED
REFUNDED
PARTIALLY_PAID
```

Trong flow hiện tại, khi MoMo fail nên để:

```text
order.paymentStatus = UNPAID
```

Lý do: đơn vẫn còn khả năng thanh toán lại hoặc đổi phương thức.

### `paymentTransaction.status`

Dùng cho từng lần thử thanh toán:

```text
PENDING
PAID
FAILED
```

Một order có thể có nhiều transaction:

```text
Order A
 -> MoMo transaction 1: FAILED
 -> MoMo transaction 2: FAILED
 -> MoMo transaction 3: PAID
```

## UI Mapping

Frontend nên đọc cả order và latest payment transaction.

API hiện có:

```http
GET /api/v1/orders/{orderId}
GET /api/v1/payments/orders/{orderId}/status
```

### Đã thanh toán

Điều kiện:

```text
order.paymentStatus = PAID
```

UI:

```text
Phương thức: MOMO
Trạng thái: Đã thanh toán
```

### Thanh toán thất bại

Điều kiện:

```text
order.paymentStatus = UNPAID
latestTransaction.status = FAILED
```

UI:

```text
Phương thức: MOMO
Trạng thái: Thanh toán thất bại
[Thanh toán lại] [Đổi phương thức]
```

### Đang chờ thanh toán online

Điều kiện:

```text
order.paymentStatus = UNPAID
latestTransaction.status = PENDING
latestTransaction.paymentMethod = MOMO
```

UI:

```text
Phương thức: MOMO
Trạng thái: Chờ thanh toán
[Tiếp tục thanh toán]
```

### Chờ thanh toán khi nhận hàng

Điều kiện:

```text
order.paymentStatus = UNPAID
order.paymentMethod = CASH hoặc COD
```

UI:

```text
Phương thức: CASH/COD
Trạng thái: Chờ thanh toán khi nhận hàng
```

## Thanh toán lại MoMo

Frontend gọi lại API tạo payment:

```http
POST /api/v1/payments/momo/create
Content-Type: application/json

{
  "orderId": "order-id"
}
```

Backend tạo một transaction mới:

```text
paymentTransaction.transactionCode = momoOrderId mới
paymentTransaction.status = PENDING
paymentTransaction.paymentMethod = MOMO
```

Transaction cũ giữ nguyên để audit:

```text
oldTransaction.status = FAILED
```

Không update đè transaction cũ.

## Đổi phương thức thanh toán

Nên bổ sung API:

```http
PATCH /api/v1/orders/{orderId}/payment-method
Content-Type: application/json

{
  "paymentMethod": "CASH"
}
```

Rule cho phép đổi:

```text
order.status = PENDING
order.paymentStatus = UNPAID
order.status != CANCELLED
order.status != REJECTED
order.status != COMPLETED
```

Sau khi đổi:

```text
order.paymentMethod = CASH
order.paymentStatus = UNPAID
```

Các MoMo transaction cũ vẫn giữ lại:

```text
MOMO transaction 1 = FAILED
MOMO transaction 2 = FAILED
```

## Timeout payment

Nên có job xử lý transaction pending quá lâu:

```text
paymentTransaction.status = PENDING
createdAt < now - 15 minutes
```

Update:

```text
paymentTransaction.status = FAILED
paymentTransaction.failedReason = Payment timeout
paymentIntent.status = FAILED
order.paymentStatus = UNPAID
```

Không hủy đơn ngay, chỉ đưa về trạng thái có thể thanh toán lại.

## Luồng đề xuất

```text
User tạo đơn MOMO
 -> order.status = PENDING
 -> order.paymentStatus = UNPAID

User bấm thanh toán
 -> BE tạo MoMo transaction PENDING
 -> FE redirect payUrl

MoMo IPN fail
 -> transaction = FAILED
 -> order.paymentStatus = UNPAID
 -> UI hiện Thanh toán thất bại

User chọn Thanh toán lại
 -> BE tạo MoMo transaction mới PENDING
 -> FE redirect payUrl mới

MoMo IPN success
 -> transaction = PAID
 -> order.paymentStatus = PAID
 -> UI hiện Đã thanh toán
```

## Checklist backend

- Không hủy order khi payment fail.
- Không update payment bằng redirectUrl.
- Chỉ update payment bằng IPN đã verify signature.
- Check IPN amount bằng transaction amount.
- Mỗi lần thanh toán lại tạo transaction mới.
- Cho đổi payment method khi order còn `PENDING` và `UNPAID`.
- Có job expire transaction `PENDING` quá hạn.

## Checklist frontend

- Không chỉ đọc `order.paymentStatus` để hiển thị lỗi thanh toán.
- Đọc thêm latest transaction để biết lần thanh toán gần nhất `FAILED` hay `PENDING`.
- Hiển thị button `Thanh toán lại` khi latest transaction `FAILED`.
- Hiển thị button `Đổi phương thức` khi order còn `PENDING` và `UNPAID`.
- Không gọi update trạng thái đơn từ returnUrl.
