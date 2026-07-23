# Tích hợp thanh toán MoMo - Tài liệu thiết kế kỹ thuật

## Phạm vi

Triển khai thanh toán MoMo AIO v2 sandbox cho các đơn hàng Pine Drink.

Luồng hỗ trợ:

1. Frontend yêu cầu URL thanh toán MoMo cho một đơn hàng hiện có.
2. Backend ký và gửi yêu cầu tạo thanh toán đến MoMo.
3. Frontend chuyển hướng khách hàng đến `payUrl`.
4. MoMo gọi IPN của backend sau khi thanh toán.
5. Backend xác minh chữ ký HMAC và cập nhật trạng thái thanh toán/đơn hàng.
6. MoMo chuyển hướng trình duyệt đến URL trả về để hiển thị kết quả giao diện.

## Cấu hình

Các giá trị được tải từ `.env` thông qua `application.yaml` với tiền tố `app.momo`.

| Biến môi trường | Mô tả |
|---|---|
| `MOMO_PARTNER_CODE` | Mã đối tác |
| `MOMO_ACCESS_KEY` | Khóa truy cập |
| `MOMO_SECRET_KEY` | Khóa bí mật |
| `MOMO_ENDPOINT` | URL tạo thanh toán MoMo |
| `MOMO_REDIRECT_URL` | URL chuyển hướng sau thanh toán (FE) |
| `MOMO_IPN_URL` | URL nhận thông báo thanh toán từ MoMo (public) |
| `MOMO_REQUEST_TYPE` | Loại yêu cầu |
| `MOMO_LANG` | Ngôn ngữ |
| `MOMO_AUTO_CAPTURE` | Tự động xác nhận |
| `MOMO_PARTNER_NAME` | Tên đối tác |
| `MOMO_STORE_ID` | Mã cửa hàng |
| `MOMO_ORDER_GROUP_ID` | Mã nhóm đơn hàng |

Để test IPN backend cục bộ, sử dụng ngrok để tạo URL công khai và đặt `MOMO_IPN_URL` tương ứng.

## API

### Tạo thanh toán MoMo

`POST /api/v1/payments/momo/create`

Xác thực: yêu cầu đăng nhập.

Yêu cầu:

```json
{
  "orderId": "order-uuid",
  "orderInfo": "Pay Pine Drink order PD123",
  "extraData": ""
}
```

Phản hồi:

```json
{
  "success": true,
  "message": "MoMo payment created successfully",
  "data": {
    "orderId": "order-uuid-1710000000000",
    "requestId": "order-uuid-1710000000000",
    "payUrl": "https://test-payment.momo.vn/...",
    "deeplink": "momo://...",
    "qrCodeUrl": "https://...",
    "resultCode": 0,
    "message": "Successful.",
    "provider": "MOMO",
    "paymentMethod": "MOMO",
    "transactionId": "transaction-uuid"
  }
}
```

Frontend chuyển hướng trình duyệt đến `data.payUrl`.

### IPN MoMo

`POST /api/v1/payments/momo/ipn`

Xác thực: công khai, được gọi bởi MoMo.

Hành vi:

- Xây dựng lại chữ ký thô MoMo từ các trường IPN.
- Xác minh HMAC SHA256 bằng `MOMO_SECRET_KEY`.
- Tìm giao dịch theo MoMo `orderId` được lưu trong `py_transaction.transaction_code`.
- Xác minh IPN `amount` khớp với số tiền giao dịch trước khi đánh dấu đã thanh toán.
- Nếu `resultCode == 0`, đặt giao dịch `PAID`, ý định `PAID`, đơn hàng `paymentStatus=PAID`.
- Nếu thất bại, đặt giao dịch `FAILED`, ý định `FAILED`, đơn hàng `paymentStatus=UNPAID`.

Phản hồi cho MoMo:

```json
{
  "partnerCode": "MOMO",
  "orderId": "order-uuid-1710000000000",
  "requestId": "order-uuid-1710000000000",
  "resultCode": 0,
  "message": "Success"
}
```

### Trả về MoMo

`GET /api/v1/payments/momo/return`

Xác thực: công khai.

Chỉ sử dụng để xác minh/hiển thị giao diện. Trạng thái đơn hàng phải dựa trên IPN, không phải URL trả về.

## Chữ ký

Chữ ký thô tạo thanh toán:

```text
accessKey={accessKey}&amount={amount}&extraData={extraData}&ipnUrl={ipnUrl}&orderId={orderId}&orderInfo={orderInfo}&partnerCode={partnerCode}&redirectUrl={redirectUrl}&requestId={requestId}&requestType={requestType}
```

Chữ ký thô IPN:

```text
accessKey={accessKey}&amount={amount}&extraData={extraData}&message={message}&orderId={orderId}&orderInfo={orderInfo}&orderType={orderType}&partnerCode={partnerCode}&payType={payType}&requestId={requestId}&responseTime={responseTime}&resultCode={resultCode}&transId={transId}
```

Cả hai đều sử dụng `HmacSHA256(secretKey)` và đầu ra hex chữ thường.

## Lưu trữ dữ liệu

- `py_payment_intent`: lưu nhà cung cấp, số tiền, payload yêu cầu, payload phản hồi, trạng thái.
- `py_transaction`: lưu mã đơn hàng MoMo dưới dạng `transaction_code`, nhà cung cấp/phương thức thanh toán `MOMO`, số tiền, trạng thái, thời gian thanh toán, lý do thất bại.
- `od_order.payment_status`: chỉ cập nhật từ IPN.

## Quy tắc số tiền

- Số tiền thanh toán sử dụng `Order.totalAmount`, đã bao gồm tổng phụ, phí vận chuyển và giảm giá voucher trong luồng đơn hàng hiện tại.
- Số tiền MoMo phải là số nguyên VND; backend sử dụng `setScale(0, RoundingMode.UNNECESSARY)` để từ chối giá trị thập phân thay vì cắt bớt.
- IPN số tiền không khớp sẽ đánh dấu giao dịch `FAILED` và trả về `resultCode=1` cho MoMo.

## Xử lý lỗi

- Nếu gọi tạo thanh toán MoMo hết thời gian, trả về 500, hoặc không thể phân tích, backend đánh dấu giao dịch và ý định `FAILED` trước khi ném ngoại lệ `PAYMENT_001`.
- Điều này ngăn các giao dịch thanh toán `PENDING` tồn đọng sau khi nhà cung cấp/mạng gặp sự cố.

## Ghi chú

- `MOMO_REDIRECT_URL` có thể là `http://localhost:4200/payment/momo-return` vì trình duyệt có thể truy cập FE localhost.
- `MOMO_IPN_URL` phải là HTTPS công khai; sử dụng ngrok cho phát triển cục bộ.
- Không bao giờ tiết lộ `MOMO_SECRET_KEY` cho frontend.
- Thông tin xác thực sản phẩm phải thay thế giá trị sandbox thông qua biến môi trường.
