# Technical Design Document — Order Expiry (Auto-Reject via RabbitMQ Delayed Message)

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-ORDER-EXPIRY-001 |
| **Project** | Pine Drink — Hệ thống order đồ uống online |
| **Module** | Order Expiry / Auto-Reject |
| **Version** | 1.1 |
| **Status** | Draft |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-06-20 |

---

## 1. Title

**Tự động từ chối đơn hàng PENDING quá hạn sử dụng bằng RabbitMQ Delayed Message**

---

## 2. Overview

Khi khách hàng tạo đơn hàng, trạng thái ban đầu là `PENDING`. Nếu sau một khoảng thời gian nhất định (mặc định: 15 phút) mà nhân viên chưa xác nhận (`CONFIRMED`), hệ thống sẽ tự động chuyển trạng thái đơn hàng sang `REJECTED` và giải phóng tồn kho đã được giữ.

Thay vì sử dụng scheduled task (cron job) query định kỳ, giải pháp chính sử dụng **RabbitMQ Delayed Message Exchange** — mỗi đơn hàng tạo ra sẽ gửi một delayed message, sau đúng `N` phút message được trả về queue và service xử lý reject.

Song song đó, một **Fallback Scheduler** chạy mỗi 5 phút sẽ query các đơn `PENDING` quá hạn và gửi lại delayed message, đảm bảo không đơn hàng nào bị treo vĩnh viễn nếu RabbitMQ restart.

---

## 3. Purpose

- Tự động từ chối đơn hàng `PENDING` quá hạn mà không cần human intervention
- Giải phóng tồn kho (`BranchVariantDailyStock`) đã reserve cho đơn hàng bị từ chối
- Ghi nhận `OrderStatusHistory` khi reject để truy vết
- Gửi thông báo realtime (WebSocket) cho khách hàng khi đơn bị reject
- Tránh việc đơn hàng "treo" mãi trong trạng thái `PENDING`

---

## 4. Scope

### Trong phạm vi (In Scope)

- Cấu hình RabbitMQ Delayed Message Exchange plugin
- Tạo exchange, queue, binding cho order expiry flow
- Gửi delayed message sau khi transaction commit khi tạo đơn hàng
- `OrderExpiryService` xử lý reject đơn hàng
- `OrderExpiryListener` nhận message từ RabbitMQ
- Fallback scheduler để xử lý đơn bị mất message
- Release stock khi reject (sử dụng ngày tạo đơn, không phải `LocalDate.now()`)
- Ghi OrderStatusHistory
- Gửi realtime notification (WebSocket event)
- Cấu hình timeout linh hoạt qua `application.yaml`

### Ngoài phạm vi (Out of Scope)

- Gửi email thông báo đơn bị reject (có thể bổ sung sau)
- Push notification (mobile)
- Hủy đơn hàng (`CANCELLED`) — chỉ xử lý `REJECTED`
- Thay đổi logic confirm/preparing/ready/existing flow

---

## 5. Audience

| Đối tượng | Vai trò |
|-----------|---------|
| Developer Backend | Implement listener, service, config RabbitMQ, sửa OrderService |
| DevOps | Cài đặt RabbitMQ plugin trên server |
| QA / Tester | Test scenario: đơn PENDING quá hạn → auto reject |
| Technical Leader | Review giải pháp |

---

## 6. Background

### 6.1 Vấn đề

Đơn hàng `PENDING` cần được nhân viên xác nhận trong thời gian hợp lý. Nếu khách hàng tạo đơn nhưng nhân viên không xử lý, đơn sẽ "treo" vô thời hạn, chiếm giữ tồn kho và gây trải nghiệm xấu.

### 6.2 Hiện trạng

- **Order Entity**: Có trường `status` (String), `rejectedAt`, `cancelReason`, `createdAt`
- **OrderStatus Enum**: Đã có `PENDING`, `REJECTED`
- **OrderService**: Đã có logic `releaseStock()` và `saveStatusHistory()`
- **OrderRepository**: Đã có `findByIdForUpdate()` cho pessimistic lock
- **RabbitMQ**: Đã cấu hình sẵn với các exchange/queue cho domain events, email, background job
- **BranchVariantDailyStockService**: Đã có `release(branchId, variantId, localDate, quantity, orderId)`
- **Chưa có**: `rabbitmq_delayed_message_exchange` plugin, order expiry service/listener, delayed message khi tạo đơn

### 6.3 Ràng buộc

- Spring Boot 3.4.5, Spring AMQP
- RabbitMQ 3.x với management plugin
- Cần cài đặt plugin `rabbitmq_delayed_message_exchange` trên RabbitMQ server
- Delay tối đa của plugin: ~40.8 ngày (2^32 - 1 ms)
- ID là String UUID (36 char)
- Pattern service: `@Service` + `@RequiredArgsConstructor` + interface/impl

---

## 7. Requirements

### 7.1 Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| FR1 | Auto-reject expired orders | Đơn hàng `PENDING` quá `expire.timeout-minutes` sẽ bị reject tự động |
| FR2 | Release stock | Khi reject, giải phóng stock đã reserve bởi `dailyStockService.release()` using `order.createdAt.toLocalDate()` |
| FR3 | Status history | Ghi nhận transition `PENDING → REJECTED` với reason "Order expired" |
| FR4 | Idempotent service | Service phải xử lý đúng nếu message bị duplicate hoặc đơn đã chuyển trạng thái |
| FR5 | Configurable timeout | Timeout có thể thay đổi qua `application.yaml` mà không cần rebuild |
| FR6 | Realtime notification | Gửi WebSocket event khi đơn bị reject để client cập nhật UI |
| FR7 | Fallback scheduler | Scheduler chạy mỗi 5 phút query đơn PENDING quá hạn → gửi lại message |

### 7.2 Non-Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| NFR1 | Reliability | Message phải được xử lý ít nhất 1 lần (at-least-once delivery) |
| NFR2 | Idempotency | Service phải idempotent — xử lý duplicate message an toàn |
| NFR3 | Performance | Không ảnh hưởng performance của order creation (< 50ms overhead) |
| NFR4 | Graceful degradation | Nếu RabbitMQ down, order creation vẫn hoạt động. Fallback scheduler xử lý |
| NFR5 | Observability | Log rõ ràng khi reject, có thể theo dõi qua RabbitMQ management UI |
| NFR6 | Transaction safety | RabbitMQ message chỉ được gửi SAU khi DB transaction commit thành công |

---

## 8. Design / Giải pháp thiết kế

### 8.1 Kiến trúc tổng quan

```
┌──────────┐     ┌──────────────────────────────────────────────────────────────────────┐
│  Client   │     │                          Backend                                    │
│ (React)   │     │                                                                      │
│           │     │  ┌──────────┐   ┌──────────────┐   ┌────────────────────────┐       │
│  create   │────►│  │  Order   │   │ OrderService │   │  OrderRepository       │       │
│  order    │     │  │Controller│──►│ + createOrder│──►│  OrderItemRepository   │       │
│           │     │  └──────────┘   └──────┬───────┘   └────────────────────────┘       │
│           │     │                        │                                             │
│           │     │                        │ @Transactional                              │
│           │     │                        │ ┌─────────────────────────────┐              │
│           │     │                        │ │ save(order)                 │              │
│           │     │                        │ │ save(orderItems)            │              │
│           │     │                        │ │ reserveStock()              │              │
│           │     │                        │ │ clearCart()                 │              │
│           │     │                        │ └─────────────┬───────────────┘              │
│           │     │                        │               │                             │
│           │     │                        │               ▼ COMMIT                        │
│           │     │                        │  ┌─────────────────────────────┐              │
│           │     │                        │  │ TransactionSynchronization  │              │
│           │     │                        │  │ .afterCommit(() ->          │              │
│           │     │                        │  │   sendOrderExpiryMessage()) │              │
│           │     │                        │  └─────────────┬───────────────┘              │
│           │     │                        │               │                             │
│           │     │                        │               ▼ SAU COMMIT                    │
│           │     │                        │  ┌─────────────────────────────┐              │
│           │     │                        │  │ RabbitTemplate              │              │
│           │     │                        │  │ .convertAndSend(delayed)    │              │
│           │     │                        │  └─────────────┬───────────────┘              │
└──────────┘     │                        │               │                             │
                  │                        └───────────────┼─────────────────────────────┘
                  │                                       │
                  │                                       ▼
                  │  ┌────────────────────────────────────────────────────────────────┐  │
                  │  │                    RabbitMQ                                    │  │
                  │  │                                                                │  │
                  │  │  ┌─────────────────────────────────────┐                      │  │
                  │  │  │ pine-drink.order-expire.exchange     │                      │  │
                  │  │  │ (x-delayed-message, durable)        │                      │  │
                  │  │  └──────────────────┬──────────────────┘                      │  │
                  │  │                     │                                           │  │
                  │  │                     ▼                                           │  │
                  │  │  ┌─────────────────────────────────────┐                      │  │
                  │  │  │ pine-drink.order-expire.queue        │                      │  │
                  │  │  │ (durable)                            │                      │  │
                  │  │  └──────────────────┬──────────────────┘                      │  │
                  │  └─────────────────────┼──────────────────────────────────────────┘  │
                  │                        │                                             │
                  │                        ▼                                             │
                  │  ┌────────────────────────────────────────────────────────────────┐  │
                  │  │  OrderExpiryListener          OrderExpiryService               │  │
                  │  │  @RabbitListener ──────────►  .expire(orderId)                 │  │
                  │  │                                        │                        │  │
                  │  │                                        ▼                        │  │
                  │  │                              ┌──────────────────┐               │  │
                  │  │                              │ Lock order       │               │  │
                  │  │                              │ Check PENDING?   │               │  │
                  │  │                              │ REJECTED         │               │  │
                  │  │                              │ releaseStock()   │               │  │
                  │  │                              │ saveHistory()    │               │  │
                  │  │                              │ publishEvent()   │               │  │
                  │  │                              └──────────────────┘               │  │
                  │  └────────────────────────────────────────────────────────────────┘  │
                  │                                                                       │
                  │  ┌────────────────────────────────────────────────────────────────┐  │
                  │  │  Fallback Scheduler (@Scheduled every 5 min)                   │  │
                  │  │  - Query PENDING orders where createdAt < cutoff               │  │
                  │  │  - Re-send delayed message for each                           │  │
                  │  │  - Safety net khi RabbitMQ message bị mất                      │  │
                  │  └────────────────────────────────────────────────────────────────┘  │
                  └──────────────────────────────────────────────────────────────────────┘
```

### 8.2 Delayed Message Exchange — Cơ chế hoạt động

RabbitMQ plugin `rabbitmq_delayed_message_exchange` cho phép gửi message với độ trễ tùy chỉnh. Message được lưu trong Mnesia table (internal storage) và được deliver sau đúng thời gian delay.

```
Producer                          RabbitMQ                           Consumer
  │                                │                                   │
  │── send(msg, delay=900000ms) ──►│                                   │
  │   header: x-delay: 900000     │                                   │
  │                                │  Store in Mnesia (in-memory)      │
  │                                │  Timer: 15 minutes                │
  │                                │                                   │
  │          ... 15 minutes ...    │                                   │
  │                                │                                   │
  │                                │── deliver(msg) ──────────────────►│
  │                                │                                   │
  │                                │                                   │── process(msg)
  │                                │                                   │   Check order status
  │                                │                                   │   Reject if PENDING
  │                                │                                   │
```

**Lưu ý quan trọng:**
- Delayed message exchange sử dụng Mnesia table để lưu message → nếu RabbitMQ restart, message chưa deliver sẽ bị mất
- Chính vì lý do này, **Fallback Scheduler là MVP bắt buộc** (xem §9.8)
- Plugin hỗ trợ maximum delay: `2^32 - 1 ms` (~49.7 ngày)
- Message được enqueue theo FIFO trong cùng 1 delay value
- Có thể dùng `x-delayed-type: direct` hoặc `topic`

### 8.3 Transaction Safety — Gửi message sau commit

**Vấn đề**: Nếu gửi RabbitMQ message bên trong `@Transactional`, có thể xảy ra:

```
1. save(order)            → DB pending
2. sendRabbitMQ(message)  → Rabbit nhận message
3. throw exception        → DB rollback
4. 15 phút sau            → Listener: order not found!
```

**Giải pháp**: Sử dụng `TransactionSynchronizationManager` để gửi message SAU khi commit:

```java
@Transactional
public OrderResponse createOrder(...) {
    // ... save order, reserve stock, clear cart ...

    Order savedOrder = orderRepository.save(order);

    // Đăng ký callback sau commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sendOrderExpiryMessage(savedOrder.getId());
            }
        }
    );

    return toOrderResponse(savedOrder);
}
```

**Flow an toàn:**

```
1. save(order)            → DB pending
2. registerSynchronization → đăng ký callback
3. return response        → Spring commit transaction
4. afterCommit()          → RabbitMQ send (DB đã committed)
5. 15 phút sau            → Listener: order tồn tại ✓
```

### 8.4 Data Flow — Chi tiết

#### 8.4.1 Order Creation + Send Delayed Message (Sau Commit)

```
POST /api/v1/orders (Authenticated)
  │
  ▼
OrderController.createOrder(request, @CurrentUser)
  │
  ▼
OrderService.createOrder(customerId, request):
  1. Validate customer, branch
  2. Lock cart (pessimistic)
  3. Calculate subtotal, discount, delivery fee
  4. Create Order (status = PENDING)
  5. Copy CartItem → OrderItem
  6. Reserve stock (dailyStockService.reserve)
  7. Save OrderStatusHistory: null → PENDING
  8. Clear cart
  9. Register TransactionSynchronization.afterCommit():
     ┌─────────────────────────────────────────────────────────┐
     │  rabbitTemplate.convertAndSend(                         │
     │    "pine-drink.order-expire.exchange",                  │
     │    "order.expire",                                      │
     │    order.getId(),                                       │
     │    message -> {                                         │
     │      message.getMessageProperties()                     │
     │        .setDelay(expireTimeoutMs);                      │
     │    }                                                    │
     │  );                                                     │
     └─────────────────────────────────────────────────────────┘
  10. Return OrderResponse
  11. Spring commits transaction
  12. afterCommit() executes → message sent to RabbitMQ
```

#### 8.4.2 OrderExpiryService — Reject Order

```
OrderExpiryListener receives message
  │
  ▼
OrderExpiryService.expire(orderId):
  1. order = orderRepository.findById(orderId)
     → Nếu không tìm thấy → log warning, return (ACK)
  2. currentStatus = order.getStatus()
  3. Nếu currentStatus != "PENDING":
     → Log: "Order {id} already processed: {status}"
     → return (ACK — order đã được confirm/reject/cancel trước đó)
  4. Begin transaction:
     a. order = orderRepository.findByIdForUpdate(orderId)  // Pessimistic lock
     b. Double-check status == PENDING (race condition safety)
     c. order.setStatus("REJECTED")
     d. order.setRejectedAt(now)
     e. order.setCancelReason("Order expired - not confirmed within {timeout} minutes")
     f. orderRepository.save(order)
     g. releaseStock(order, order.getCreatedAt().toLocalDate())  // ← DÙNG NGÀY TẠO ĐƠN
     h. saveStatusHistory(order, "PENDING", "REJECTED", reason)
     i. Publish domain event → WebSocket / Email / Notification
  5. Log: "Order {orderCode} auto-rejected: expired"
```

#### 8.4.3 Release Stock — Fix ngày reserve/release mismatch

**Bug cũ:**

```java
// RELEASE đang dùng LocalDate.now()
dailyStockService.release(branchId, variantId, LocalDate.now(), quantity, orderId);

// Nhưng RESERVE dùng ngày tạo đơn (có thể là hôm qua)
dailyStockService.reserve(branchId, variantId, order.getCreatedAt().toLocalDate(), quantity, orderId);
```

Nếu order tạo hôm nay (2026-06-19) nhưng listener chạy ngày mai (2026-06-20):
- Reserve: `2026-06-19` → OK
- Release: `2026-06-20` → Sai ngày, stock lệch!

**Fix:**

```java
private void releaseStock(Order order, LocalDate stockDate) {
    List<OrderItem> items = orderItemRepository.findByOrderIdWithVariant(order.getId());
    for (OrderItem item : items) {
        if (item.getVariant() != null) {
            dailyStockService.release(
                order.getBranch().getId(),
                item.getVariant().getId(),
                stockDate,            // ← DÙNG NGÀY TẠO ĐƠN
                item.getQuantity(),
                order.getId()
            );
        }
    }
}

// Gọi:
releaseStock(order, order.getCreatedAt().toLocalDate());
```

### 8.5 Sequence Diagrams

#### 8.5.1 Create Order + Send Delayed Message (Sau Commit)

```
Client                OrderController       OrderService          TransactionSync      RabbitTemplate
  │                        │                    │                      │                     │
  │── POST /orders ───────►│                    │                      │                     │
  │                        │── createOrder ────►│                      │                     │
  │                        │                    │── validate ─────────►│                     │
  │                        │                    │── save(order) ──────►│                     │
  │                        │                    │── reserve stock ────►│                     │
  │                        │                    │── clear cart ───────►│                     │
  │                        │                    │                      │                     │
  │                        │                    │── register ─────────►│                     │
  │                        │                    │   afterCommit()      │                     │
  │                        │                    │                      │                     │
  │                        │◄── OrderResponse ──│                      │                     │
  │◄── BaseResponse.ok ────│                    │                      │                     │
  │                        │                    │                      │                     │
  │                        │                    │  [COMMIT DB]         │                     │
  │                        │                    │                      │── afterCommit() ───►│
  │                        │                    │                      │                     │── convertAndSend
  │                        │                    │                      │                     │   delay: 900000ms
  │                        │                    │                      │                     │
```

#### 8.5.2 Listener → Service Reject Flow

```
RabbitMQ              OrderExpiryListener       OrderExpiryService       OrderRepository      StockService
  │                        │                          │                       │                     │
  │── deliver(msg) ──────►│                          │                       │                     │
  │   body: orderId        │                          │                       │                     │
  │                        │── expire(orderId) ──────►│                       │                     │
  │                        │                          │── findById(orderId) ─►│                     │
  │                        │                          │◄── Order ─────────────│                     │
  │                        │                          │                       │                     │
  │                        │                          │  Check: status == PENDING?                   │
  │                        │                          │  ├─ No → return (ACK)                        │
  │                        │                          │  └─ Yes ↓                                    │
  │                        │                          │                       │                     │
  │                        │                          │── findByIdForUpdate ─►│                     │
  │                        │                          │◄── Order (locked) ────│                     │
  │                        │                          │                       │                     │
  │                        │                          │  Double-check: status == PENDING?           │
  │                        │                          │  ├─ No → rollback, return                    │
  │                        │                          │  └─ Yes ↓                                    │
  │                        │                          │                       │                     │
  │                        │                          │── setStatus(REJECTED)►│                     │
  │                        │                          │── save(order) ───────►│                     │
  │                        │                          │                       │                     │
  │                        │                          │── releaseStock ───────────────────────────►│
  │                        │                          │   date: order.getCreatedAt().toLocalDate()   │
  │                        │                          │                       │                     │
  │                        │                          │── saveStatusHistory ─►│                     │
  │                        │                          │   PENDING → REJECTED  │                     │
  │                        │                          │                       │                     │
  │                        │                          │── publishEvent ────► (Domain Event)         │
  │                        │                          │   order.rejected       │                     │
  │                        │                          │                       │                     │
  │                        │◄── return ───────────────│                       │                     │
  │                        │                          │                       │                     │
  │                        │── ACK ──────────────────►│                       │                     │
```

### 8.6 RabbitMQ Configuration

#### 8.6.1 Exchange

| Property | Value |
|----------|-------|
| Name | `pine-drink.order-expire.exchange` |
| Type | `x-delayed-message` |
| Durable | `true` |
| Arguments | `x-delayed-type: direct` |

#### 8.6.2 Queue

| Property | Value |
|----------|-------|
| Name | `pine-drink.order-expire.queue` |
| Durable | `true` |
| Dead Letter Exchange | `pine-drink.order-expire.exchange` |
| Dead Letter Routing Key | `order.expire.dlq` |

#### 8.6.3 Binding

| Property | Value |
|----------|-------|
| Source | `pine-drink.order-expire.exchange` |
| Destination | `pine-drink.order-expire.queue` |
| Routing Key | `order.expire` |

### 8.7 Application Configuration

```yaml
app:
  rabbitmq:
    order-expiry:
      exchange: ${APP_RABBITMQ_ORDER_EXPIRY_EXCHANGE:pine-drink.order-expire.exchange}
      queue: ${APP_RABBITMQ_ORDER_EXPIRY_QUEUE:pine-drink.order-expire.queue}
      routing-key: ${APP_RABBITMQ_ORDER_EXPIRY_ROUTING_KEY:order.expire}

order:
  expire:
    timeout-minutes: ${ORDER_EXPIRE_TIMEOUT_MINUTES:15}
```

### 8.8 Error Codes

Bổ sung vào `ErrorCode.java`:

```java
ORDER_002("ORDER_002", "Order has expired and was auto-rejected"),
ORDER_003("ORDER_003", "Order is no longer in PENDING status"),
```

---

## 9. Details / Chi tiết triển khai

### 9.1 Docker Compose — Enable Plugin

**File**: `docker-compose.yml`

```yaml
rabbitmq:
  image: rabbitmq:3-management-alpine
  container_name: pine-drink-rabbitmq
  restart: unless-stopped
  command: >
    sh -c "rabbitmq-plugins enable --offline
           rabbitmq_stomp
           rabbitmq_web_stomp
           rabbitmq_delayed_message_exchange
         && rabbitmq-server"
  environment:
    RABBITMQ_DEFAULT_USER: ${RABBITMQ_DEFAULT_USER}
    RABBITMQ_DEFAULT_PASS: ${RABBITMQ_DEFAULT_PASS}
  ports:
    - "${RABBITMQ_PORT}:5672"
    - "${RABBITMQ_STOMP_PORT:-61613}:61613"
    - "${RABBITMQ_MANAGEMENT_PORT}:15672"
    - "${RABBITMQ_WEB_STOMP_PORT:-15674}:15674"
  volumes:
    - rabbitmq_data:/var/lib/rabbitmq
  networks:
    - pine-drink-network
```

**Thay đổi**: Thêm `rabbitmq_delayed_message_exchange` vào danh sách plugins.

### 9.2 RabbitMqProperties — Thêm OrderExpiry Channel

**File**: `configuration/RabbitMqProperties.java`

```java
@ConfigurationProperties(prefix = "app.rabbitmq")
public record RabbitMqProperties(
        Stomp stomp,
        Realtime realtime,
        Channel domainEvents,
        Channel email,
        Channel backgroundJob,
        OrderExpiry orderExpiry          // ← THÊM MỚI
) {
    // ... existing records ...

    public record OrderExpiry(
            String exchange,
            String queue,
            String routingKey
    ) {
    }
}
```

### 9.3 RabbitMqConfig — Thêm Exchange, Queue, Binding

**File**: `configuration/RabbitMqConfig.java`

```java
// ──────────────────────────────────────────────
// 6. Order Expiry — Delayed Message Exchange
// ──────────────────────────────────────────────

@Bean
public DirectExchange orderExpireExchange() {
    return ExchangeBuilder
            .directExchange(properties.orderExpiry().exchange())
            .delayed()                              // x-delayed-message type
            .delayArgument("x-delay")               // argument name for delay
            .durable(true)
            .build();
}

@Bean
public Queue orderExpireQueue() {
    return QueueBuilder.durable(properties.orderExpiry().queue())
            .withArgument("x-dead-letter-exchange", properties.orderExpiry().exchange())
            .withArgument("x-dead-letter-routing-key", "order.expire.dlq")
            .build();
}

@Bean
public Binding orderExpireBinding() {
    return BindingBuilder.bind(orderExpireQueue())
            .to(orderExpireExchange())
            .with(properties.orderExpiry().routingKey());
}
```

### 9.4 OrderProperties — Thêm Expire Config

**File**: `configuration/OrderProperties.java`

```java
@Configuration
@ConfigurationProperties(prefix = "order")
@Getter
@Setter
public class OrderProperties {

    private Delivery delivery = new Delivery();
    private Cancel cancel = new Cancel();
    private Expire expire = new Expire();        // ← THÊM MỚI

    // ... existing classes ...

    @Getter
    @Setter
    public static class Expire {
        /**
         * Timeout in minutes for auto-rejecting PENDING orders
         */
        private Integer timeoutMinutes = 15;
    }
}
```

### 9.5 OrderService — Gửi Delayed Message SAU commit

**File**: `service/impl/OrderServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;
    private final OrderProperties orderProperties;

    // ... existing fields ...

    @Override
    @Transactional
    public OrderResponse createOrder(String customerId, CreateOrderRequest request) {
        // ... existing logic (validate, create order, save, etc.) ...

        Order order = new Order();
        // ... set fields ...
        order = orderRepository.save(order);
        saveStatusHistory(order, null, OrderStatus.PENDING.getValue(), "Order created");
        saveVoucherUsage(request.getVoucherCode(), order, customer, discountAmount);
        saveDelivery(request, order);

        // ... copy cart items to order items, reserve stock, clear cart ...

        log.info("Order created successfully: orderId={}, orderCode={}", order.getId(), order.getOrderCode());

        // ── Đăng ký gửi delayed message SAU KHI COMMIT ──
        String orderId = order.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        sendOrderExpiryMessage(orderId);
                    }
                }
        );

        return toOrderResponse(order);
    }

    private void sendOrderExpiryMessage(String orderId) {
        try {
            int delayMs = orderProperties.getExpire().getTimeoutMinutes() * 60 * 1000;

            rabbitTemplate.convertAndSend(
                    rabbitMqProperties.orderExpiry().exchange(),
                    rabbitMqProperties.orderExpiry().routingKey(),
                    orderId,
                    message -> {
                        message.getMessageProperties().setDelay(delayMs);
                        return message;
                    }
            );

            log.info("Order expiry message sent: orderId={}, delay={}ms", orderId, delayMs);
        } catch (Exception e) {
            // Log error nhưng KHÔNG throw — order đã tạo thành công
            // Fallback scheduler sẽ xử lý nếu message bị mất
            log.error("Failed to send order expiry message: orderId={}", orderId, e);
        }
    }
}
```

### 9.6 OrderExpiryService — Tách business logic khỏi Listener

**File**: `service/OrderExpiryService.java` (TẠO MỚI — interface)

```java
package com.hoandev.pinedrink.service;

public interface OrderExpiryService {
    /**
     * Expire a PENDING order that was not confirmed within the allowed time.
     * Idempotent: does nothing if order is not found or already processed.
     *
     * @param orderId the order ID to expire
     */
    void expire(String orderId);
}
```

**File**: `service/impl/OrderExpiryServiceImpl.java` (TẠO MỚI — implementation)

```java
package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.OrderItem;
import com.hoandev.pinedrink.entity.OrderStatusHistory;
import com.hoandev.pinedrink.entity.enums.OrderStatus;
import com.hoandev.pinedrink.repository.OrderItemRepository;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.repository.OrderStatusHistoryRepository;
import com.hoandev.pinedrink.service.BranchVariantDailyStockService;
import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryServiceImpl implements OrderExpiryService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final BranchVariantDailyStockService dailyStockService;
    private final OrderProperties orderProperties;

    @Override
    @Transactional
    public void expire(String orderId) {
        log.info("Processing order expiry: orderId={}", orderId);

        Order order = orderRepository.findById(orderId)
                .orElse(null);

        if (order == null) {
            log.warn("Order not found for expiry: orderId={}", orderId);
            return;
        }

        String currentStatus = order.getStatus();

        // Nếu đơn không còn PENDING → đã xử lý rồi, bỏ qua
        if (!OrderStatus.PENDING.getValue().equals(currentStatus)) {
            log.info("Order {} already processed: status={}", orderId, currentStatus);
            return;
        }

        // Lock order và double-check status (tránh race condition)
        order = orderRepository.findByIdForUpdate(orderId)
                .orElse(null);

        if (order == null || !OrderStatus.PENDING.getValue().equals(order.getStatus())) {
            log.info("Order {} already processed after lock: status={}",
                    orderId, order != null ? order.getStatus() : "NOT_FOUND");
            return;
        }

        // Reject order
        LocalDateTime now = LocalDateTime.now();
        Integer timeoutMinutes = orderProperties.getExpire().getTimeoutMinutes();
        order.setStatus(OrderStatus.REJECTED.getValue());
        order.setRejectedAt(now);
        order.setCancelReason("Order expired - not confirmed within " + timeoutMinutes + " minutes");
        orderRepository.save(order);

        // Release reserved stock — dùng ngày tạo đơn, KHÔNG dùng LocalDate.now()
        releaseStock(order);

        // Save status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(OrderStatus.PENDING.getValue());
        history.setNewStatus(OrderStatus.REJECTED.getValue());
        history.setReason("Order expired - auto rejected after " + timeoutMinutes + " minutes");
        orderStatusHistoryRepository.save(history);

        log.info("Order auto-rejected: orderId={}, orderCode={}, createdAt={}",
                order.getId(), order.getOrderCode(), order.getCreatedAt());

        // TODO: Publish domain event → WebSocket / Email / Notification
        // domainEventPublisher.publish("order.rejected", order.getId());
    }

    /**
     * Release stock using the order's creation date (not LocalDate.now()).
     * This ensures the release date matches the reserve date.
     */
    private void releaseStock(Order order) {
        // Dùng ngày tạo đơn để match với ngày reserve stock
        LocalDate stockDate = order.getCreatedAt().toLocalDate();

        var items = orderItemRepository.findByOrderIdWithVariant(order.getId());
        for (var item : items) {
            if (item.getVariant() != null) {
                dailyStockService.release(
                        order.getBranch().getId(),
                        item.getVariant().getId(),
                        stockDate,              // ← NGÀY TẠO ĐƠN, KHÔNG PHẢI LocalDate.now()
                        item.getQuantity(),
                        order.getId()
                );
            }
        }
    }
}
```

### 9.7 OrderExpiryListener — Thin listener, delegate to service

**File**: `listener/OrderExpiryListener.java` (TẠO MỚI)

```java
package com.hoandev.pinedrink.listener;

import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryListener {

    private final OrderExpiryService orderExpiryService;

    @RabbitListener(
            queues = "${app.rabbitmq.order-expiry.queue:pine-drink.order-expire.queue}",
            concurrency = "1-3"
    )
    public void handleOrderExpiry(String orderId) {
        log.info("Received order expiry message: orderId={}", orderId);
        orderExpiryService.expire(orderId);
    }
}
```

**Tại sao Listener thin?**

Listener chỉ làm 2 việc:
1. Nhận message từ RabbitMQ
2. Delegate sang `OrderExpiryService`

Khi cần thêm logic (email, notification, analytics), chỉ cần sửa `OrderExpiryServiceImpl`. Listener không thay đổi.

### 9.8 Fallback Scheduler — MVP Bắt buộc

**Vấn đề**: Delayed message exchange sử dụng Mnesia table (in-memory). Nếu RabbitMQ restart, message chưa deliver sẽ bị mất → đơn hàng treo vĩnh viễn.

**Giải pháp**: Scheduler chạy mỗi 5 phút query đơn `PENDING` quá hạn → gửi lại delayed message.

**File**: `scheduler/OrderExpiryFallbackScheduler.java` (TẠO MỚI)

```java
package com.hoandev.pinedrink.scheduler;

import com.hoandev.pinedrink.configuration.OrderProperties;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.service.OrderExpiryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "order.expire.fallback-enabled",
        havingValue = "true",
        matchIfMissing = true    // BẬT mặc định
)
public class OrderExpiryFallbackScheduler {

    private final OrderRepository orderRepository;
    private final OrderExpiryService orderExpiryService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitMqProperties rabbitMqProperties;
    private final OrderProperties orderProperties;

    /**
     * Chạy mỗi 5 phút.
     * Query đơn PENDING quá hạn → gửi lại delayed message.
     * Nếu message vẫn bị mất → order sẽ hết hạn khi listener nhận được.
     */
    @Scheduled(fixedDelay = 300000)  // 5 phút
    public void handleExpiredOrders() {
        log.debug("Running order expiry fallback scheduler...");

        int timeoutMinutes = orderProperties.getExpire().getTimeoutMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(timeoutMinutes);

        // Tìm đơn PENDING quá hạn (chưa bị reject bởi delayed message)
        List<Order> expiredOrders = orderRepository
                .findByStatusAndCreatedAtBefore("PENDING", cutoff);

        if (expiredOrders.isEmpty()) {
            log.debug("No expired orders found");
            return;
        }

        log.info("Found {} expired PENDING orders, re-sending expiry messages",
                expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Gửi lại delayed message (delay = 0, xử lý ngay)
                rabbitTemplate.convertAndSend(
                        rabbitMqProperties.orderExpiry().exchange(),
                        rabbitMqProperties.orderExpiry().routingKey(),
                        order.getId(),
                        message -> {
                            // Delay = 0 → xử lý ngay khi nhận được
                            message.getMessageProperties().setDelay(0);
                            return message;
                        }
                );
                log.info("Re-sent expiry message for order: orderId={}, orderCode={}",
                        order.getId(), order.getOrderCode());
            } catch (Exception e) {
                log.error("Failed to re-send expiry message for order: orderId={}",
                        order.getId(), e);
            }
        }
    }
}
```

**Lưu ý quan trọng:**

- Fallback scheduler gửi message với `delay = 0` → listener xử lý ngay
- Listener sẽ gọi `OrderExpiryService.expire()` → check status → reject nếu vẫn PENDING
- Đây là safety net, không phải luồng chính
- Có thể tắt bằng `order.expire.fallback-enabled=false`

**Cần thêm repository method:**

```java
// OrderRepository.java
List<Order> findByStatusAndCreatedAtBefore(String status, LocalDateTime createdAt);
```

### 9.9 ErrorCode — Thêm mã lỗi mới

**File**: `exception/ErrorCode.java`

```java
// Thêm sau ORDER_001
ORDER_002("ORDER_002", "Order has expired and was auto-rejected"),
ORDER_003("ORDER_003", "Order is no longer in PENDING status"),
```

### 9.10 EnableScheduling — Bật scheduling

**File**: `PineDrinkApplication.java`

```java
@SpringBootApplication
@EnableScheduling    // ← THÊM MỚI cho fallback scheduler
public class PineDrinkApplication {
    public static void main(String[] args) {
        SpringApplication.run(PineDrinkApplication.class, args);
    }
}
```

---

## 10. Edge Cases & Xử lý

| # | Trường hợp | Xử lý |
|---|-----------|-------|
| 1 | **Order đã CONFIRMED trước khi message đến** | Service check status → != PENDING → return, ACK |
| 2 | **Order đã CANCELLED trước khi message đến** | Tương tự case 1 |
| 3 | **Message bị duplicate (at-least-once)** | Service double-check status sau khi lock → idempotent |
| 4 | **Order không tồn tại (đã xóa mềm)** | Service findById = null → log warning → return |
| 5 | **RabbitMQ restart trước khi deliver** | **Fallback scheduler** query và gửi lại message (delay=0) |
| 6 | **Race condition: staff confirm đồng thời** | Pessimistic lock (`findByIdForUpdate`) + double-check status |
| 7 | **RabbitMQ down khi gửi message** | `sendOrderExpiryMessage` catch exception → log error, fallback scheduler xử lý |
| 8 | **Delay quá ngắn (< 1s)** | Plugin minimum delay = 0, practical minimum ~ 1s |
| 9 | **Nhiều instance app (horizontal scaling)** | RabbitMQ load-balances consumers, fallback scheduler query across all instances |
| 10 | **Transaction rollback sau khi register callback** | `afterCommit()` chỉ chạy khi commit thành công → message không bị gửi |
| 11 | **Stock release sai ngày** | Dùng `order.getCreatedAt().toLocalDate()` thay vì `LocalDate.now()` |

---

## 11. Ma Trạng Thái — State Transition

```
                    ┌──────────────┐
                    │   PENDING    │
                    └──────┬───────┘
                           │
            ┌──────────────┼──────────────┐
            │              │              │
            ▼              ▼              ▼
    ┌──────────────┐ ┌──────────┐ ┌──────────────┐
    │  CONFIRMED   │ │ REJECTED │ │  CANCELLED   │
    │ (by staff)   │ │ (by exp.)│ │ (by customer)│
    └──────┬───────┘ └──────────┘ └──────────────┘
           │              ▲              ▲
           ▼              │              │
    ┌──────────────┐      │              │
    │  PREPARING   │      │              │
    └──────┬───────┘      │              │
           │              │              │
           ▼              │              │
    ┌──────────────┐      │              │
    │    READY     │──────┘              │
    └──────┬───────┘                     │
           │                             │
           ▼                             │
    ┌──────────────┐                     │
    │  DELIVERING  │─────────────────────┘
    └──────┬───────┘
           │
           ▼
    ┌──────────────┐
    │  COMPLETED   │
    └──────────────┘
```

**Transition mới**: `PENDING → REJECTED` (bởi order expiry service)

---

## 12. So sánh: Trước vs Sau khi sửa

| Vấn đề | Trước (v1.0) | Sau (v1.1) |
|--------|-------------|------------|
| **Gửi message** | Trong `@Transactional` | Sau commit via `TransactionSynchronization.afterCommit()` |
| **Release stock ngày** | `LocalDate.now()` (sai) | `order.getCreatedAt().toLocalDate()` (đúng) |
| **Fallback scheduler** | "Future improvement" (§12.2) | **MVP bắt buộc** (§9.8) |
| **Listener** | Làm tất cả (reject + stock + history + ws) | Thin listener → delegate `OrderExpiryService` |
| **Architecture** | 1 class làm hết | Listener → Service → (future: event → email/ws) |

---

## 13. Flow Tổng Quan — Final

```
Create Order
    │
    ▼
Commit DB (order, items, stock reserved)
    │
    ▼
afterCommit() → Send Delayed Message (15 min)
    │
    ▼
┌─────────────────────────────────────────┐
│           RabbitMQ                       │
│  Mnesia storage: 15 min countdown        │
└───────────────┬─────────────────────────┘
                │
                │ (nếu RabbitMQ restart → message mất)
                │
                ▼
┌─────────────────────────────────────────┐
│     Fallback Scheduler (5 min)           │
│  Query PENDING orders > cutoff           │
│  Re-send message with delay=0            │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  OrderExpiryListener                     │
│  @RabbitListener                         │
│  → OrderExpiryService.expire(orderId)   │
└───────────────┬─────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────┐
│  OrderExpiryService.expire()             │
│  ├─ Lock order (findByIdForUpdate)       │
│  ├─ Check: PENDING?                      │
│  │   ├─ No → return (already processed)  │
│  │   └─ Yes ↓                            │
│  ├─ Set status = REJECTED                │
│  ├─ Release stock (createdAt.toLocalDate)│
│  ├─ Save status history                  │
│  └─ Publish domain event                 │
│      ├─ WebSocket → Client UI update     │
│      ├─ Email → Customer notification    │
│      └─ Analytics → Dashboard            │
└─────────────────────────────────────────┘
```

---

## 14. Checklist Triển Khai

| # | Task | File | Status |
|---|------|------|--------|
| 1 | Enable `rabbitmq_delayed_message_exchange` plugin | `docker-compose.yml` | TODO |
| 2 | Thêm `OrderExpiry` record vào `RabbitMqProperties` | `RabbitMqProperties.java` | TODO |
| 3 | Thêm config `app.rabbitmq.order-expiry.*` | `application.yaml` | TODO |
| 4 | Thêm exchange, queue, binding beans | `RabbitMqConfig.java` | TODO |
| 5 | Thêm `OrderProperties.Expire` class | `OrderProperties.java` | TODO |
| 6 | Thêm config `order.expire.timeout-minutes` | `application.yaml` | TODO |
| 7 | **Sửa** `createOrder`: gửi message sau commit | `OrderServiceImpl.java` | TODO |
| 8 | Tạo `OrderExpiryService` (interface + impl) | `service/` | TODO |
| 9 | Tạo `OrderExpiryListener` (thin) | `listener/` | TODO |
| 10 | Tạo `OrderExpiryFallbackScheduler` | `scheduler/` | TODO |
| 11 | Thêm `@EnableScheduling` | `PineDrinkApplication.java` | TODO |
| 12 | Thêm repository method `findByStatusAndCreatedAtBefore` | `OrderRepository.java` | TODO |
| 13 | Thêm error codes `ORDER_002`, `ORDER_003` | `ErrorCode.java` | TODO |
| 14 | Test: Đơn PENDING quá hạn → auto reject (RabbitMQ) | Manual / Integration test | TODO |
| 15 | Test: Đơn CONFIRMED trước hạn → không reject | Manual / Integration test | TODO |
| 16 | Test: RabbitMQ restart → fallback scheduler xử lý | Manual test | TODO |
| 17 | Test: Transaction rollback → message không gửi | Unit test | TODO |

---

## 15. Ghi Chú

- `TransactionSynchronizationManager.registerSynchronization()` là cách an toàn nhất trong Spring để gửi message sau commit. `@TransactionalEventListener(phase = AFTER_COMMIT)` cũng có thể dùng nhưng yêu cầu Spring Event (ApplicationEventPublisher) — phù hợp hơn cho intra-app communication.
- Fallback scheduler sử dụng `@ConditionalOnProperty(name = "order.expire.fallback-enabled", havingValue = "true", matchIfMissing = true)` — bật mặc định, có thể tắt trong test.
- `order.getCreatedAt().toLocalDate()` đảm bảo release stock đúng ngày đã reserve. Đây là bug thực tế dễ gặp khi dùng `LocalDate.now()` trong listener chạy vào ngày hôm sau.
- RabbitMQ retry config: max 3 lần, initial interval 3s, multiplier 2. Sau max retries, message vào DLQ hoặc bị nack tùy cấu hình.
- Delayed message exchange KHÔNG persistent như classic queue — chính vì vậy fallback scheduler là **MVP bắt buộc**, không phải future improvement.
