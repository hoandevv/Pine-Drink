# Plan 04.1 - COD/CASH Payment Technical Design

## Goal
Implement the first payment slice for offline payment methods: `CASH` and `COD`.

This slice records payment against an existing order, creates payment intent and transaction records, and updates `od_order.payment_status`. It intentionally avoids external providers, callbacks, refunds, and payment gateway signatures.

## Current Context
- Orders are created from cart through `OrderService.createOrder`.
- New orders are stored with:
  - `status = PENDING`
  - `payment_method = request.paymentMethod`
  - `payment_status = UNPAID`
- Payment tables already exist in `V7__create_vc_payment_schema.sql`:
  - `py_payment_intent`
  - `py_transaction`
  - `py_callback_log`
  - `py_refund`
- `PaymentIntent` and `PaymentTransaction` inherit `BaseEntity.status`; this maps to the payment table `status` columns.

## Supported Methods

### CASH
Used when the customer pays directly at store counter.

Expected flow:
1. Customer creates order with `paymentMethod = CASH`.
2. Staff receives cash.
3. Staff records payment.
4. Backend creates or reuses `PaymentIntent`.
5. Backend creates or updates `PaymentTransaction`.
6. Backend sets:
   - `PaymentTransaction.status = PAID`
   - `Order.paymentStatus = PAID`

### COD
Used when the customer pays on delivery.

Expected flow:
1. Customer creates order with `paymentMethod = COD`.
2. Delivery/staff receives money.
3. Staff records payment.
4. Backend stores transaction and sets order payment as paid.

## API

### Record Offline Payment

`POST /api/v1/payments/offline/record`

Authorization:
- `PERM_ORDER_UPDATE_STATUS`

Request:

```json
{
  "orderId": "order-uuid",
  "paymentMethod": "CASH"
}
```

Allowed `paymentMethod` values:
- `CASH`
- `COD`

Response:

```json
{
  "id": "transaction-uuid",
  "transactionCode": "PAY-...",
  "orderId": "order-uuid",
  "orderCode": "ORD-...",
  "provider": "CASH",
  "paymentMethod": "CASH",
  "amount": 59000,
  "currency": "VND",
  "status": "PAID",
  "orderPaymentStatus": "PAID",
  "paidAt": "2026-06-25T10:30:00"
}
```

### Get Order Payment Status

`GET /api/v1/payments/orders/{orderId}/status`

Authorization:
- `PERM_ORDER_VIEW`
- `PERM_ORDER_VIEW_BRANCH`
- `PERM_ORDER_VIEW_OWN`

Response returns the latest transaction for the order when one exists.

## Business Rules
- Only `CASH` and `COD` are accepted in this slice.
- Cancelled or rejected orders cannot be marked paid.
- The request method must match the order `paymentMethod`.
- If the order is already `PAID`, the operation returns the latest paid transaction and does not create another charge.
- If an offline transaction already exists for the same order and method, it is reused and marked `PAID`.
- Updating `paymentStatus` does not automatically move order workflow status. Staff still uses the order status endpoint for `CONFIRMED`, `PREPARING`, `READY`, etc.

## Implementation Files
- `controller/PaymentController.java`
- `service/PaymentService.java`
- `service/impl/PaymentServiceImpl.java`
- `entity/dto/request/Payment/RecordOfflinePaymentRequest.java`
- `entity/dto/response/Payment/PaymentTransactionResponse.java`
- `repository/PaymentIntentRepository.java`
- `repository/PaymentTransactionRepository.java`

## Follow-up
- Add payment-specific permissions such as `PERM_PAYMENT_RECORD` and `PERM_PAYMENT_VIEW`.
- Add provider callbacks for VNPAY and MoMo.
- Add refund flow and refund permissions.
- Publish `payment.success` events for notification and reporting.
