# Plan 3 — Cart & Order Processing

## Mục tiêu
Xây dựng Cart và Order flow hoàn chỉnh: guest cart (sessionId), logged-in cart (customerId), add/update/remove items, apply voucher, create order với đầy đủ validation, cancel order.

## Files cần tạo/sửa

### Services
- `service/CartService.java` + `service/impl/CartServiceImpl.java`
- `service/OrderService.java` + `service/impl/OrderServiceImpl.java`
- `service/VoucherService.java` + `service/impl/VoucherServiceImpl.java`

### Controllers
- `controller/CartController.java`
- `controller/OrderController.java`

### Mappers
- `mapper/OrderMapper.java`
- `mapper/CartMapper.java`

## Chi tiết Implementation

### Cart Flow

```
Guest:       sessionId (từ cookie/header) → CartService tạo/find cart
Logged-in:   customerId (từ JWT) → CartService tạo/find cart

Add item:    validate product availability tại branch
             validate variant/topping tồn tại
             save CartItem + CartItemTopping
             recalculate total_price

Update item: thay đổi quantity → recalculate
Remove item: xóa CartItem → recalculate
```

#### CartService.addItem()

```java
@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired private CartRepository cartRepository;
    @Autowired private CartItemRepository cartItemRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ToppingRepository toppingRepository;
    @Autowired private BranchProductAvailabilityRepository availabilityRepo;
    @Autowired private CartMapper cartMapper;

    @Override
    public CartResponse addItem(AddCartItemRequest request, String sessionId, Long customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);

        // Validate branch
        if (cart.getBranchId() == null) {
            cart.setBranchId(request.getBranchId());
        } else if (!cart.getBranchId().equals(request.getBranchId())) {
            // Nếu đổi branch → clear cart cũ
            cartItemRepository.deleteByCartId(cart.getId());
            cart.setBranchId(request.getBranchId());
        }

        // Validate product availability tại branch
        BranchProductAvailability availability =
                availabilityRepo.findByBranchIdAndProductId(
                        request.getBranchId(), request.getProductId())
                        .orElseThrow(() -> new BadRequestException(
                                "Product not available at this branch"));

        if (!availability.isAvailable()) {
            throw new BadRequestException("Product is currently unavailable at this branch");
        }

        // Validate variant
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Variant not found: " + request.getVariantId()));
        }

        // Validate toppings
        List<Topping> toppings = Collections.emptyList();
        if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
            toppings = toppingRepository.findAllById(request.getToppingIds());
            if (toppings.size() != request.getToppingIds().size()) {
                throw new BadRequestException("One or more toppings not found");
            }
        }

        // Create cart item
        CartItem item = CartItem.builder()
                .cart(cart)
                .productId(request.getProductId())
                .productName(request.getProductName())
                .variantId(request.getVariantId())
                .variantName(variant != null ? variant.getName() : null)
                .quantity(request.getQuantity())
                .unitPrice(calculateUnitPrice(request.getProductId(), request.getVariantId()))
                .totalPrice(calculateTotalPrice(request.getQuantity(),
                        request.getProductId(), request.getVariantId(), toppings))
                .note(request.getNote())
                .build();

        item = cartItemRepository.save(item);

        // Save toppings
        if (!toppings.isEmpty()) {
            List<CartItemTopping> itemToppings = toppings.stream()
                    .map(t -> CartItemTopping.builder()
                            .cartItem(item)
                            .toppingId(t.getId())
                            .toppingName(t.getName())
                            .toppingPrice(t.getPrice())
                            .build())
                    .collect(Collectors.toList());
            cartItemRepository.saveAllToppings(itemToppings);
        }

        // Recalculate cart total
        recalculateCartTotal(cart);

        return cartMapper.toResponse(cart);
    }

    private BigDecimal calculateUnitPrice(Long productId, Long variantId) {
        // Logic: base price + variant additional price
    }

    private BigDecimal calculateTotalPrice(int quantity, Long productId,
                                            Long variantId, List<Topping> toppings) {
        BigDecimal unitPrice = calculateUnitPrice(productId, variantId);
        BigDecimal toppingsPrice = toppings.stream()
                .map(Topping::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return unitPrice.add(toppingsPrice).multiply(BigDecimal.valueOf(quantity));
    }
}
```

### Order Flow

```
Validate: voucher, stock, branch hours, product availability
Save: order + items + toppings + status_history (PENDING)
Tính: subtotal = sum(items.total_price)
      discount = voucher discount
      total = subtotal - discount + delivery_fee
Trừ voucher usage count (atomic)
Response: order với status PENDING
```

#### OrderService.createOrder()

```java
@Override
@Transactional
public OrderResponse createOrder(CreateOrderRequest request, Long customerId) {
    Cart cart = cartRepository.findByCustomerId(customerId)
            .orElseThrow(() -> new BadRequestException("Cart is empty"));

    if (cart.getItems().isEmpty()) {
        throw new BadRequestException("Cart is empty");
    }

    // Validate branch hoạt động
    Branch branch = branchRepository.findById(cart.getBranchId())
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));

    // Validate giờ hoạt động
    if (!branch.isOpenNow()) {
        throw new BusinessException("Branch is currently closed");
    }

    // Tính toán
    BigDecimal subtotal = calculateSubtotal(cart);
    BigDecimal deliveryFee = calculateDeliveryFee(branch, request.getDeliveryAddress());
    BigDecimal discount = BigDecimal.ZERO;
    String voucherCode = null;

    // Validate + apply voucher
    if (request.getVoucherCode() != null) {
        VoucherValidationResult voucherResult =
                voucherService.validateAndApply(request.getVoucherCode(),
                        subtotal, customerId, cart.getBranchId());
        discount = voucherResult.getDiscountAmount();
        voucherCode = request.getVoucherCode();
    }

    BigDecimal total = subtotal.subtract(discount).add(deliveryFee);

    // Tạo Order
    Order order = Order.builder()
            .customerId(customerId)
            .branchId(cart.getBranchId())
            .orderCode(generateOrderCode())
            .status(OrderStatus.PENDING)
            .subtotal(subtotal)
            .discount(discount)
            .deliveryFee(deliveryFee)
            .total(total)
            .voucherCode(voucherCode)
            .deliveryAddress(request.getDeliveryAddress())
            .note(request.getNote())
            .build();

    order = orderRepository.save(order);

    // Copy cart items → order items
    for (CartItem cartItem : cart.getItems()) {
        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .productId(cartItem.getProductId())
                .productName(cartItem.getProductName())
                .variantId(cartItem.getVariantId())
                .variantName(cartItem.getVariantName())
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .totalPrice(cartItem.getTotalPrice())
                .note(cartItem.getNote())
                .build();

        orderItem = orderItemRepository.save(orderItem);

        // Copy toppings
        for (CartItemTopping cartTopping : cartItem.getToppings()) {
            OrderItemTopping orderTopping = OrderItemTopping.builder()
                    .orderItem(orderItem)
                    .toppingId(cartTopping.getToppingId())
                    .toppingName(cartTopping.getToppingName())
                    .toppingPrice(cartTopping.getToppingPrice())
                    .build();
            orderItemRepository.saveTopping(orderTopping);
        }
    }

    // Save status history
    OrderStatusHistory history = OrderStatusHistory.builder()
            .order(order)
            .fromStatus(null)
            .toStatus(OrderStatus.PENDING)
            .changedBy(customerId)
            .build();
    orderStatusHistoryRepository.save(history);

    // Clear cart
    cartItemRepository.deleteByCartId(cart.getId());
    cartRepository.delete(cart);

    return orderMapper.toResponse(order);
}
```

### VoucherService.validateAndApply()

```java
@Override
@Transactional
public VoucherValidationResult validateAndApply(String code, BigDecimal subtotal,
                                                  Long customerId, Long branchId) {
    Voucher voucher = voucherRepository.findByCodeAndStatus(code, Constants.STATUS_ACTIVE)
            .orElseThrow(() -> new BadRequestException("Invalid voucher code"));

    // Check hạn sử dụng
    LocalDateTime now = LocalDateTime.now();
    if (now.isBefore(voucher.getStartDate()) || now.isAfter(voucher.getEndDate())) {
        throw new BadRequestException("Voucher has expired or not yet active");
    }

    // Check usage limit
    if (voucher.getUsageLimit() != null &&
            voucher.getUsageCount() >= voucher.getUsageLimit()) {
        throw new BadRequestException("Voucher usage limit reached");
    }

    // Check min order
    if (voucher.getMinOrderAmount() != null &&
            subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
        throw new BadRequestException(
                "Minimum order amount is " + voucher.getMinOrderAmount());
    }

    // Check branch scope
    if (voucher.getScope() == VoucherScope.BRANCH) {
        boolean validForBranch = voucherBranchRepository
                .existsByVoucherIdAndBranchId(voucher.getId(), branchId);
        if (!validForBranch) {
            throw new BadRequestException("Voucher not valid for this branch");
        }
    }

    // Tính discount
    BigDecimal discountAmount;
    if (voucher.getDiscountType() == DiscountType.PERCENTAGE) {
        discountAmount = subtotal.multiply(
                voucher.getDiscountValue().divide(BigDecimal.valueOf(100)));
        // Capped bởi max_discount_amount
        if (voucher.getMaxDiscountAmount() != null &&
                discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
            discountAmount = voucher.getMaxDiscountAmount();
        }
    } else {
        discountAmount = voucher.getDiscountValue(); // FIXED_AMOUNT
    }

    // Atomic increment usage count
    voucherRepository.incrementUsageCount(voucher.getId());

    return VoucherValidationResult.builder()
            .voucherId(voucher.getId())
            .discountAmount(discountAmount)
            .build();
}
```

### Cancel Order

```java
@Override
@Transactional
public void cancelOrder(Long orderId, Long customerId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

    if (!order.getCustomerId().equals(customerId)) {
        throw new UnauthorizedException("You can only cancel your own orders");
    }

    if (order.getStatus() != OrderStatus.PENDING) {
        throw new BadRequestException("Can only cancel PENDING orders");
    }

    order.setStatus(OrderStatus.CANCELLED);
    orderRepository.save(order);

    // Refund voucher usage count nếu có
    if (order.getVoucherCode() != null) {
        voucherRepository.decrementUsageCountByCode(order.getVoucherCode());
    }

    // Save history
    OrderStatusHistory history = OrderStatusHistory.builder()
            .order(order)
            .fromStatus(OrderStatus.PENDING)
            .toStatus(OrderStatus.CANCELLED)
            .changedBy(customerId)
            .reason("Customer cancelled")
            .build();
    orderStatusHistoryRepository.save(history);
}
```

### Entity Relationships

```
Order (1) ──── (N) OrderItem (1) ──── (N) OrderItemTopping
  ├── customerId
  ├── branchId
  ├── orderCode (unique)
  ├── status (enum)
  ├── subtotal, discount, delivery_fee, total
  ├── voucherCode
  ├── deliveryAddress
  └── note

OrderItem
  ├── productId, productName
  ├── variantId, variantName
  ├── quantity, unitPrice, totalPrice
  └── note

OrderItemTopping
  ├── toppingId, toppingName
  └── toppingPrice
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | /api/v1/cart | Get current cart (sessionId hoặc JWT) |
| POST | /api/v1/cart/items | Add item to cart |
| PUT | /api/v1/cart/items/{itemId} | Update cart item quantity |
| DELETE | /api/v1/cart/items/{itemId} | Remove item from cart |
| POST | /api/v1/cart/apply-voucher | Apply voucher |
| DELETE | /api/v1/cart/apply-voucher | Remove voucher |
| GET | /api/v1/cart/summary | Get cart summary (subtotal, discount, total) |
| POST | /api/v1/orders | Create order từ cart |
| GET | /api/v1/orders | Get user's orders (phân trang) |
| GET | /api/v1/orders/{orderId} | Get order detail |
| POST | /api/v1/orders/{orderId}/cancel | Cancel order (chỉ PENDING) |

## Checklist

- [ ] Tạo CartService: findOrCreateCart, addItem, updateItem, removeItem
- [ ] Tạo CartController endpoints
- [ ] Xử lý guest cart (sessionId) vs logged-in cart (customerId)
- [ ] Validate product/variant/topping availability khi add item
- [ ] Cart item recalculation (quantity change → total_price)
- [ ] Tạo CartMapper + response DTOs
- [ ] Tạo OrderService: createOrder với đầy đủ validation
- [ ] Validate branch hours, stock, voucher
- [ ] Copy cart → order items + toppings
- [ ] Tính toán subtotal, discount, delivery fee, total
- [ ] Atomic decrement voucher usage count
- [ ] Order status history tracking
- [ ] Clear cart sau khi tạo order thành công
- [ ] Cancel order (chỉ khi PENDING)
- [ ] Refund voucher usage khi cancel
- [ ] Tạo OrderController endpoints
- [ ] Test full flow: add cart → apply voucher → create order → cancel
