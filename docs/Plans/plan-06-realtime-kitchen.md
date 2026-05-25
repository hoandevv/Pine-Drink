# Plan 6 — Realtime Kitchen & Staff Flow

## Mục tiêu
Xây dựng Staff/Kitchen screen API và WebSocket realtime để nhân viên có thể quản lý đơn hàng theo thời gian thực.

## Files cần tạo

| File | Mô tả |
|------|-------|
| `configuration/WebSocketConfig.java` | Cấu hình STOMP + SockJS message broker |
| `controller/StaffOrderController.java` | REST controller cho staff order operations |
| `service/StaffOrderService.java` | Interface StaffOrderService |
| `service/impl/StaffOrderServiceImpl.java` | Implementation StaffOrderService |
| `websocket/OrderWebSocketHandler.java` | Xử lý WebSocket messages (hoặc dùng `@MessageMapping` trong controller) |

## Chi tiết implementation

### 1. WebSocketConfig — STOMP + SockJS

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

### 2. Staff Order Flow — Status Transitions

```
PENDING → CONFIRMED → PREPARING → READY → COMPLETED
  ↓ (cancel)
CANCELLED
```

### 3. StaffOrderController

```java
@RestController
@RequestMapping("/staff/orders")
@RequiredArgsConstructor
public class StaffOrderController {

    private final StaffOrderService staffOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrders(
            @RequestParam UUID branchId,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.getOrders(branchId, status)));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.confirmOrder(id)));
    }

    @PatchMapping("/{id}/preparing")
    public ResponseEntity<ApiResponse<OrderResponse>> startPreparing(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.startPreparing(id)));
    }

    @PatchMapping("/{id}/ready")
    public ResponseEntity<ApiResponse<OrderResponse>> markReady(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.markReady(id)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.completeOrder(id)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody @Valid CancelOrderRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(staffOrderService.cancelOrder(id, request.reason())));
    }
}
```

### 4. StaffOrderServiceImpl — WebSocket publish after status change

```java
@Service
@RequiredArgsConstructor
public class StaffOrderServiceImpl implements StaffOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public OrderResponse confirmOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Only PENDING orders can be confirmed");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Ghi status history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(order.getId())
                .fromStatus(OrderStatus.PENDING)
                .toStatus(OrderStatus.CONFIRMED)
                .changedBy(OrderStatusHistory.ChangedBy.STAFF)
                .build();
        statusHistoryRepository.save(history);

        // Publish WebSocket
        publishStatusChange(order, OrderStatus.CONFIRMED);

        return OrderResponse.fromEntity(order);
    }

    private void publishStatusChange(Order order, OrderStatus newStatus) {
        OrderStatusMessage message = OrderStatusMessage.builder()
                .type("STATUS_CHANGED")
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .branchId(order.getBranchId())
                .newStatus(newStatus.name())
                .timestamp(LocalDateTime.now())
                .build();

        // Gửi tới staff của branch
        messagingTemplate.convertAndSend(
            "/topic/orders/" + order.getBranchId(), message);

        // Gửi tới customer
        messagingTemplate.convertAndSend(
            "/topic/my-orders/" + order.getCustomerId(), message);

        // Gửi tới topic chi tiết đơn
        messagingTemplate.convertAndSend(
            "/topic/orders/" + order.getId() + "/status", message);
    }
}
```

### 5. WebSocket message format

```json
{
  "type": "NEW_ORDER | STATUS_CHANGED",
  "orderId": "uuid",
  "orderCode": "PD-XXXXX",
  "branchId": "uuid",
  "customerId": "uuid",
  "newStatus": "PENDING | CONFIRMED | PREPARING | READY | COMPLETED | CANCELLED",
  "timestamp": "2026-05-24T10:30:00"
}
```

### 6. Security — WebSocket interceptor JWT

```java
@Component
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        String token = accessor.getFirstNativeHeader("Authorization");

        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtTokenProvider.validateToken(token)) {
                Claims claims = jwtTokenProvider.getClaims(token);
                accessor.setUser(() -> claims.getSubject());
                return message;
            }
        }

        // Cũng hỗ trợ token trong query params (cho SockJS)
        if (accessor.getCommand() == StompCommand.CONNECT) {
            String queryToken = accessor.getFirstNativeHeader("token");
            if (queryToken != null && jwtTokenProvider.validateToken(queryToken)) {
                return message;
            }
        }

        throw new AuthenticationException("Invalid JWT token");
    }
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/staff/orders?branchId=&status=` | Danh sách đơn theo branch + filter status |
| PATCH | `/staff/orders/{id}/confirm` | Xác nhận đơn → CONFIRMED |
| PATCH | `/staff/orders/{id}/preparing` | Bắt đầu pha chế → PREPARING |
| PATCH | `/staff/orders/{id}/ready` | Hoàn thành → READY |
| PATCH | `/staff/orders/{id}/complete` | Bàn giao → COMPLETED |
| PATCH | `/staff/orders/{id}/cancel` | Hủy đơn → CANCELLED + cancel_reason |

## WebSocket Topics

| Topic | Subscriber | Mô tả |
|-------|-----------|-------|
| `/topic/orders/{branchId}` | Staff | Nhận đơn mới / status change theo branch |
| `/topic/my-orders/{customerId}` | Customer | Theo dõi trạng thái đơn của mình |
| `/topic/orders/{orderId}/status` | Cả hai | Chi tiết thay đổi trạng thái 1 đơn |

## Checklist

- [ ] Tạo WebSocketConfig với STOMP + SockJS
- [ ] Tạo JwtChannelInterceptor kiểm tra JWT
- [ ] Tạo StaffOrderController với đầy đủ endpoints
- [ ] Tạo StaffOrderService interface + impl
- [ ] Implement status transitions với validation
- [ ] Ghi OrderStatusHistory mỗi lần đổi status
- [ ] Publish WebSocket message sau mỗi status change
- [ ] Test manual với Postman + WebSocket client
