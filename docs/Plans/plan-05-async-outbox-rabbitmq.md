# Plan 5 — Async Processing & Outbox Pattern

## Mục tiêu
Xây dựng transactional outbox pattern để đảm bảo message không bị mất khi publish RabbitMQ thất bại. Xử lý các event: order.created, order.confirmed, order.ready, payment.success, payment.failed. Kèm WebSocket push realtime đến kitchen screen và customer.

## Files cần tạo/sửa

### Outbox
- `entity/OutboxEvent.java` — entity lưu event chờ publish
- `repository/OutboxEventRepository.java`
- `service/OutboxService.java` — publish events từ outbox
- `scheduler/OutboxPollerScheduler.java` — @Scheduled poll + publish

### Consumers
- `consumer/OrderEventConsumer.java` — xử lý order.created, order.confirmed, order.ready
- `consumer/PaymentEventConsumer.java` — xử lý payment.success, payment.failed
- `consumer/NotificationConsumer.java` — gửi SMS/email/push notification

### WebSocket
- `configuration/WebSocketConfig.java` — STOMP over SockJS
- `websocket/OrderWebSocketHandler.java` — push order status changes

## Chi tiết Implementation

### Outbox Pattern

#### Tại sao cần Outbox?

Khi save entity vào DB thành công nhưng publish RabbitMQ thất bại → message bị mất.
Outbox pattern giải quyết: save entity + outbox event trong cùng 1 @Transaction → @Scheduled poll → publish → update status.

#### Flow

```
1. Service save entity + OutboxEvent trong 1 @Transaction
2. @Scheduled poller chạy mỗi 2 giây, query events với status = PENDING
3. Lock event (set locked_by + locked_at) để tránh multiple instances
4. Publish lên RabbitMQ
5. Nếu success → update status = PUBLISHED
6. Nếu fail → increment retry_count, exponential backoff
7. Sau 5 lần retry → status = FAILED (cần manual handle)
```

#### OutboxEvent Entity

```java
@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String aggregateType; // "ORDER", "PAYMENT"

    @Column(nullable = false)
    private String aggregateId;   // orderId, transactionId

    @Column(nullable = false)
    private String eventType;     // "order.created", "payment.success"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String payload;       // JSON

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;  // PENDING, PUBLISHED, FAILED

    private int retryCount;

    private String lockedBy;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        status = OutboxStatus.PENDING;
        retryCount = 0;
    }
}
```

#### OutboxPollerScheduler

```java
@Component
public class OutboxPollerScheduler {

    private static final int MAX_RETRIES = 5;
    private static final long LOCK_TIMEOUT_SECONDS = 60;

    @Autowired private OutboxEventRepository outboxRepository;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void pollOutboxEvents() {
        String instanceId = UUID.randomUUID().toString();
        LocalDateTime lockThreshold = LocalDateTime.now()
                .minusSeconds(LOCK_TIMEOUT_SECONDS);

        // Lấy events chưa publish hoặc locked quá hạn
        List<OutboxEvent> events = outboxRepository
                .findPendingEventsWithExpiredLock(lockThreshold,
                        PageRequest.of(0, 50));

        for (OutboxEvent event : events) {
            // Lock event
            int locked = outboxRepository.lockEvent(event.getId(), instanceId, LocalDateTime.now());
            if (locked == 0) continue; // Ai đó đã lock trước

            try {
                // Publish lên RabbitMQ
                rabbitTemplate.convertAndSend(
                        getExchange(event.getEventType()),
                        event.getEventType(),
                        event.getPayload());

                // Update status thành PUBLISHED
                outboxRepository.markAsPublished(event.getId(), LocalDateTime.now());

            } catch (Exception e) {
                log.error("Failed to publish outbox event {}: {}", event.getId(), e.getMessage());

                // Retry với exponential backoff
                int newRetryCount = event.getRetryCount() + 1;
                if (newRetryCount >= MAX_RETRIES) {
                    outboxRepository.markAsFailed(event.getId());
                    log.error("Outbox event {} failed after {} retries", event.getId(), MAX_RETRIES);
                } else {
                    outboxRepository.incrementRetry(event.getId(), newRetryCount);
                }
            } finally {
                // Release lock
                outboxRepository.releaseLock(event.getId(), instanceId);
            }
        }
    }

    private String getExchange(String eventType) {
        if (eventType.startsWith("payment.")) {
            return "pine.direct";
        }
        return "pine.topic";
    }
}
```

### OrderEventConsumer

```java
@Component
public class OrderEventConsumer {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private NotificationService notificationService;

    @RabbitListener(queues = "order.event")
    public void handleOrderCreated(String payload) {
        try {
            OrderEvent event = new ObjectMapper().readValue(payload, OrderEvent.class);

            // Push WebSocket đến kitchen screen
            messagingTemplate.convertAndSend(
                    "/topic/kitchen/orders", event);

            // Push WebSocket đến customer
            messagingTemplate.convertAndSend(
                    "/topic/orders/" + event.getCustomerId(), event);

            // Gửi SMS/email xác nhận
            notificationService.sendOrderConfirmation(event.getCustomerId(),
                    event.getOrderCode(), event.getStatus());

        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage());
            // Ném exception để RabbitMQ retry
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }

    @RabbitListener(queues = "order.event")
    public void handleOrderConfirmed(OrderEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/orders/" + event.getCustomerId(), event);
    }

    @RabbitListener(queues = "order.event")
    public void handleOrderReady(OrderEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/orders/" + event.getCustomerId(), event);
        notificationService.sendOrderReadyNotification(
                event.getCustomerId(), event.getOrderCode());
    }
}
```

### PaymentEventConsumer

```java
@Component
public class PaymentEventConsumer {

    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OutboxService outboxService;

    @RabbitListener(queues = "payment.callback")
    public void handlePaymentSuccess(PaymentSuccessEvent event) {
        // Cập nhật order status nếu chưa được cập nhật
        Order order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() == OrderStatus.PENDING) {
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.save(order);

            // Tạo outbox event cho order.confirmed
            outboxService.createEvent("ORDER", order.getId().toString(),
                    "order.confirmed", order.toJson());

            // Push WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/kitchen/orders", new OrderEvent(order));
        }
    }

    @RabbitListener(queues = "payment.callback")
    public void handlePaymentFailed(PaymentFailedEvent event) {
        // Thông báo cho customer
        messagingTemplate.convertAndSend(
                "/topic/payment/failed/" + event.getTransactionId(), event);
    }
}
```

### NotificationConsumer

```java
@Component
public class NotificationConsumer {

    @Autowired private JavaMailSender mailSender;
    @Autowired private SmsService smsService;
    @Autowired private FcmService fcmService;

    @RabbitListener(queues = "notification")
    public void handleNotification(NotificationEvent event) {
        switch (event.getChannel()) {
            case EMAIL -> {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(event.getRecipient());
                helper.setSubject(event.getSubject());
                helper.setText(event.getBody(), true);
                mailSender.send(message);
            }
            case SMS -> smsService.send(event.getRecipient(), event.getBody());
            case PUSH -> fcmService.send(event.getDeviceToken(),
                    event.getSubject(), event.getBody());
        }
    }
}
```

### WebSocketConfig

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

### RabbitMQ Retry & DLQ

```java
@Bean
public Queue orderEventQueue() {
    return QueueBuilder.durable("order.event")
            .withArgument("x-dead-letter-exchange", "pine.dlx")
            .withArgument("x-dead-letter-routing-key", "order.event.dlq")
            .build();
}

@Bean
public Queue orderEventDlq() {
    return QueueBuilder.durable("order.event.dlq").build();
}

@Bean
public DirectExchange dlxExchange() {
    return new DirectExchange("pine.dlx");
}

@Bean
public Binding orderEventDlqBinding() {
    return BindingBuilder.bind(orderEventDlq())
            .to(dlxExchange())
            .with("order.event.dlq");
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| WS | /ws | WebSocket STOMP endpoint (SockJS) |
| WS | /topic/kitchen/orders | Kitchen screen order updates |
| WS | /topic/orders/{customerId} | Customer order status updates |
| WS | /topic/payment/failed/{transactionId} | Payment failure notifications |

## Checklist

- [ ] Tạo OutboxEvent entity + repository
- [ ] Tạo OutboxService createEvent()
- [ ] Tạo OutboxPollerScheduler: poll mỗi 2s, lock, publish, retry
- [ ] Exponential backoff retry tối đa 5 lần
- [ ] Lock cơ chế (locked_by + locked_at) tránh double processing
- [ ] Tạo OrderEventConsumer: order.created, order.confirmed, order.ready
- [ ] Tạo PaymentEventConsumer: payment.success, payment.failed
- [ ] Tạo NotificationConsumer: email, SMS, push
- [ ] Tạo WebSocketConfig: STOMP over SockJS
- [ ] Push kitchen screen notifications
- [ ] Push customer order status notifications
- [ ] RabbitMQ DLQ (dead letter queue) cho message failed
- [ ] Retry queues với delay (1s, 10s, 60s)
- [ ] Manual ack: channel.basicAck / basicNack
- [ ] Tích hợp outbox vào OrderService.createOrder() — save event cùng transaction
- [ ] Tích hợp outbox vào PaymentService — save payment event
- [ ] Test: save entity + outbox trong 1 transaction
- [ ] Test: poller publish và update status
- [ ] Test: retry khi RabbitMQ down
- [ ] Test: WebSocket push realtime
