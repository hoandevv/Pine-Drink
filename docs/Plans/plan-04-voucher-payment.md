# Plan 4 — Voucher & Payment

## Mục tiêu
Xây dựng voucher management (CRUD brand scope, tính discount) và payment integration cho 3 phương thức: Cash, MOMO, VNPAY. Kèm theo RabbitMQ config cho async payment callbacks.

## Files cần tạo/sửa

### Services
- `service/PaymentService.java` + `service/impl/PaymentServiceImpl.java`

### Controllers
- `controller/VoucherController.java` — admin CRUD + validate endpoint
- `controller/PaymentController.java` — create intent, callback

### Configuration
- `configuration/RabbitMQConfig.java` — queues, exchanges, bindings

### DTOs
- `entity/dto/request/CreatePaymentIntentRequest.java`
- `entity/dto/response/PaymentIntentResponse.java`
- `entity/dto/request/MomoCallbackRequest.java`
- `entity/dto/request/VnpayCallbackRequest.java`

## Chi tiết Implementation

### Voucher Management

#### Voucher CRUD (Brand scope)

```java
@RestController
@RequestMapping("/api/v1/admin/vouchers")
public class VoucherController {

    @Autowired private VoucherService voucherService;

    @PostMapping
    public VoucherResponse create(@Valid @RequestBody CreateVoucherRequest request) {
        return voucherService.create(request);
    }

    @PutMapping("/{id}")
    public VoucherResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateVoucherRequest request) {
        return voucherService.update(id, request);
    }

    @GetMapping
    public Page<VoucherResponse> getAll(@PageableDefault Pageable pageable) {
        return voucherService.getAll(pageable);
    }

    @GetMapping("/{id}")
    public VoucherResponse getById(@PathVariable Long id) {
        return voucherService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        voucherService.delete(id);
    }

    @PostMapping("/validate")
    public VoucherValidationResult validate(@Valid @RequestBody ValidateVoucherRequest request) {
        return voucherService.validate(request.getCode(), request.getSubtotal(),
                request.getCustomerId(), request.getBranchId());
    }
}
```

### Discount Calculation

```java
public BigDecimal calculateDiscount(Voucher voucher, BigDecimal subtotal) {
    BigDecimal discountAmount;

    if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
        discountAmount = subtotal.multiply(
                voucher.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));

        // Capped bởi max_discount_amount
        if (voucher.getMaxDiscountAmount() != null &&
                discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discountAmount = voucher.getMaxDiscountAmount();
        }
    } else {
        // FIXED_AMOUNT
        discountAmount = voucher.getDiscountValue();

        // Không vượt quá subtotal
        if (discountAmount.compareTo(subtotal) > 0) {
            discountAmount = subtotal;
        }
    }

    return discountAmount;
}
```

### Payment Flow

```
Cash:       tạo transaction → provider=CASH → status=PAID → cập nhật order
MOMO:       tạo payment intent → gọi MOMO API → lấy payUrl → redirect user → callback → cập nhật
VNPAY:      tạo payment intent → tạo VNPAY URL → redirect user → IPN callback → cập nhật

Idempotency: kiểm tra pf_idempotency_key trước mỗi request → tránh double charge
```

#### PaymentService.createPaymentIntent()

```java
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    @Autowired private TransactionRepository transactionRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MomoClient momoClient;
    @Autowired private VnpayClient vnpayClient;
    @Autowired private RabbitTemplate rabbitTemplate;

    @Override
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        // Idempotency check
        if (request.getIdempotencyKey() != null) {
            Transaction existing = transactionRepository
                    .findByIdempotencyKey(request.getIdempotencyKey());
            if (existing != null) {
                return mapToResponse(existing);
            }
        }

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        // Kiểm tra order chưa thanh toán
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Order is not in PENDING status");
        }

        // Tạo transaction
        Transaction transaction = Transaction.builder()
                .orderId(order.getId())
                .amount(order.getTotal())
                .provider(request.getPaymentMethod().name())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(request.getIdempotencyKey())
                .build();
        transaction = transactionRepository.save(transaction);

        PaymentIntentResponse.PaymentIntentResponseBuilder builder =
                PaymentIntentResponse.builder()
                        .transactionId(transaction.getId())
                        .amount(order.getTotal());

        switch (request.getPaymentMethod()) {
            case CASH -> {
                transaction.setStatus(TransactionStatus.PAID);
                transaction.setPaidAt(LocalDateTime.now());
                transactionRepository.save(transaction);

                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);

                builder.status("PAID");
                builder.redirectUrl(null);

                // Publish event
                rabbitTemplate.convertAndSend("pine.direct", "payment.success",
                        new PaymentSuccessEvent(transaction.getId(), order.getId()));
            }
            case MOMO -> {
                MomoPaymentResponse momoResponse = momoClient.createPayment(
                        order.getOrderCode(), order.getTotal(), request.getReturnUrl());
                transaction.setProviderTransactionId(momoResponse.getOrderId());
                transactionRepository.save(transaction);

                builder.status("PENDING");
                builder.redirectUrl(momoResponse.getPayUrl());
            }
            case VNPAY -> {
                String vnpayUrl = vnpayClient.createPaymentUrl(
                        order.getOrderCode(), order.getTotal(),
                        request.getReturnUrl(), request.getIpAddress());
                transaction.setProviderTransactionId(order.getOrderCode());
                transactionRepository.save(transaction);

                builder.status("PENDING");
                builder.redirectUrl(vnpayUrl);
            }
        }

        return builder.build();
    }
}
```

#### processMomoCallback()

```java
@Override
@Transactional
public void processMomoCallback(MomoCallbackRequest callback) {
    // Verify signature
    if (!momoClient.verifySignature(callback)) {
        throw new BadRequestException("Invalid MOMO signature");
    }

    Transaction transaction = transactionRepository
            .findByProviderTransactionId(callback.getOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

    if (transaction.getStatus() != TransactionStatus.PENDING) {
        return; // Idempotent: chỉ xử lý transaction PENDING
    }

    if ("0".equals(callback.getResultCode())) {
        // Success
        transaction.setStatus(TransactionStatus.PAID);
        transaction.setPaidAt(LocalDateTime.now());
        transaction.setProviderResponse(callback.toJson());
        transactionRepository.save(transaction);

        Order order = orderRepository.findById(transaction.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Publish event
        rabbitTemplate.convertAndSend("pine.direct", "payment.success",
                new PaymentSuccessEvent(transaction.getId(), order.getId()));
    } else {
        // Failed
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setProviderResponse(callback.toJson());
        transactionRepository.save(transaction);

        rabbitTemplate.convertAndSend("pine.direct", "payment.failed",
                new PaymentFailedEvent(transaction.getId(), callback.getMessage()));
    }
}
```

### RabbitMQConfig

```java
@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_DIRECT = "pine.direct";
    public static final String EXCHANGE_TOPIC = "pine.topic";
    public static final String QUEUE_PAYMENT_CALLBACK = "payment.callback";
    public static final String QUEUE_NOTIFICATION = "notification";
    public static final String ROUTING_PAYMENT_SUCCESS = "payment.success";
    public static final String ROUTING_PAYMENT_FAILED = "payment.failed";

    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange(EXCHANGE_DIRECT);
    }

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(EXCHANGE_TOPIC);
    }

    @Bean
    public Queue paymentCallbackQueue() {
        return QueueBuilder.durable(QUEUE_PAYMENT_CALLBACK)
                .withArgument("x-dead-letter-exchange", EXCHANGE_DIRECT + ".dlq")
                .withArgument("x-dead-letter-routing-key", QUEUE_PAYMENT_CALLBACK + ".dlq")
                .build();
    }

    @Bean
    public Binding paymentSuccessBinding() {
        return BindingBuilder.bind(paymentCallbackQueue())
                .to(directExchange())
                .with(ROUTING_PAYMENT_SUCCESS);
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentCallbackQueue())
                .to(directExchange())
                .with(ROUTING_PAYMENT_FAILED);
    }

    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setTypeResolver(new DefaultJackson2JavaTypeMapper());
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| CRUD | /api/v1/admin/vouchers | Admin voucher management |
| POST | /api/v1/admin/vouchers/validate | Validate voucher (test) |
| POST | /api/v1/payments/create-intent | Create payment intent |
| GET | /api/v1/payments/{transactionId}/status | Check transaction status |
| POST | /api/v1/payments/momo/callback | MOMO IPN callback |
| POST | /api/v1/payments/vnpay/callback | VNPAY IPN callback |
| GET | /api/v1/payments/vnpay/return | VNPAY return URL (frontend redirect) |

## Checklist

- [ ] Tạo VoucherController: CRUD voucher với brand scope
- [ ] Gán voucher cho branch qua VoucherBranch entity
- [ ] Validate voucher: date range, usage limit, min_order, max_discount
- [ ] Tính discount: PERCENTAGE (capped) hoặc FIXED_AMOUNT
- [ ] Tạo PaymentService interface + impl
- [ ] Cash payment: tạo transaction, set PAID, confirm order
- [ ] MOMO: create payment intent → gọi MOMO API → payUrl
- [ ] MOMO: verify signature → process callback → update transaction
- [ ] VNPAY: create payment URL → redirect
- [ ] VNPAY: IPN callback → verify → update transaction
- [ ] Idempotency key check (tránh double charge)
- [ ] Tạo RabbitMQConfig: exchanges, queues, bindings, DLQ
- [ ] Publish payment.success / payment.failed events
- [ ] Tạo Transaction entity + repository
- [ ] Test Cash payment flow
- [ ] Test MOMO full flow (create → callback → update)
- [ ] Test VNPAY full flow (create → callback → update)
- [ ] Test idempotency (gửi request 2 lần với cùng key)
