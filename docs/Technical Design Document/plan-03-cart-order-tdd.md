# Technical Design Document — Cart & Order Processing

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-CART-001 |
| **Project** | Pine Drink — Hệ thống order đồ uống online |
| **Module** | Cart & Order Processing |
| **Version** | 1.0 |
| **Status** | Draft |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-06-05 |

---

## 1. Title

**Hệ thống Cart, Voucher & Order Processing cho Pine Drink Platform**

---

## 2. Overview

Pine Drink là hệ thống order đồ uống online no-brand, branch-first. Tài liệu này mô tả chi tiết giải pháp xử lý giỏ hàng (cart), áp dụng voucher/khuyến mãi, và tạo đơn hàng (order) cho toàn bộ hệ thống.

Hệ thống hỗ trợ 2 loại người dùng chính khi thao tác với cart:

- **Guest**: Khách chưa đăng nhập, cart gắn với `sessionId` (lưu trong cookie/header)
- **Logged-in user**: Khách đã đăng nhập, cart gắn với `customerId` (từ JWT)

Flow tổng quan:

```
Guest / Logged-in → Cart (add/update/remove items, apply voucher) → Create Order → Payment
```

---

## 3. Purpose

- Cho phép guest và logged-in user thao tác với cart (thêm/sửa/xoá sản phẩm)
- Validate product/variant/topping availability tại branch khi add item
- Tính toán lại unit price, total price khi thay đổi quantity
- Áp dụng voucher với đầy đủ validation (hạn sử dụng, usage limit, min order, branch scope)
- Tạo order từ cart: copy snapshot items + tính toán subtotal, discount, delivery fee, total
- Track trạng thái order qua `OrderStatusHistory`
- Cancel order (chỉ PENDING) và refund voucher usage
- Hỗ trợ sugar level (`SugarLevel`) và ice level (`IceLevel`) cho mỗi cart item / order item

---

## 4. Scope

### Trong phạm vi (In Scope)

- Cart CRUD: add/update/remove items, get cart
- Cart phân loại: guest (`sessionId`) vs logged-in (`customerId`)
- Product availability validation tại branch
- Variant, topping validation
- Tính toán unit price, total price real-time
- Voucher: validate, apply, remove, atomic usage count
- Order: create từ cart, get list, get detail
- Cancel order (PENDING) + refund voucher
- Order status history tracking
- Cart → Order snapshot copy
- Clear cart sau khi tạo order thành công

### Ngoài phạm vi (Out of Scope)

- Payment gateway integration (MOMO, VNPAY, v.v.)
- Order delivery management (shipper assignment, delivery tracking)
- Kitchen screen / real-time order push
- Admin order management (confirm, prepare, complete)
- WebSocket notifications for order status changes

---

## 5. Audience

| Đối tượng | Vai trò |
|-----------|---------|
| Developer Backend | Implement services, controllers, mappers, DTOs |
| Developer Frontend | Tích hợp API cart/order/voucher |
| QA / Tester | Viết test case cho cart và order flow |
| Technical Leader | Review giải pháp |

---

## 6. Background

### 6.1 Vấn đề

Hệ thống Pine Drink cần xử lý luồng đặt hàng hoàn chỉnh từ lúc khách chọn sản phẩm đến lúc tạo đơn hàng. Cart và Order là module core của business.

### 6.2 Hiện trạng

- **Entities đã có**: `Cart`, `CartItem`, `CartItemTopping`, `Order`, `OrderItem`, `OrderItemTopping`, `OrderStatusHistory`, `Voucher`, `VoucherBranch`, `VoucherUsage`, `PaymentIntent`, `PaymentTransaction`, `CallbackLog`, `Refund`
- **Repositories đã có**: tất cả repository interfaces tương ứng
- **Enums đã có**: `OrderStatus`, `OrderType`, `PaymentStatus`, `PaymentProvider`, `DiscountType`, `SugarLevel`, `IceLevel`, `EntityStatus`
- **Chưa có**: DTOs (request/response), Services (CartService, OrderService, VoucherService), Controllers, Mappers

### 6.3 Ràng buộc

- Spring Boot 4.0.6, Spring Security 6.x
- JPA + Hibernate với MySQL
- ID là String UUID (36 char), không phải Long
- Pattern mapper: `@Component`, không dùng MapStruct
- Pattern service: `@Service` + `@RequiredArgsConstructor` + interface/impl
- Pattern API response: `BaseResponse<T>`
- Authorization: `hasAuthority('PERM_*')` + `accessScopeService.assert*()`
- Cart không persist lượng lớn dữ liệu (in-memory scaling không cần thiết)

---

## 7. Requirements

### 7.1 Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| FR1 | Guest cart | Guest có thể thao tác cart thông qua `sessionId` gửi từ client (cookie/header) |
| FR2 | Logged-in cart | User đã đăng nhập có cart gắn với `customerId` |
| FR3 | Merge cart | Khi guest login, guest cart được merge/logged-in cart được ưu tiên (optional phase) |
| FR4 | Add item | Thêm sản phẩm + variant + toppings vào cart, validate availability tại branch |
| FR5 | Update item | Thay đổi quantity của cart item, recalculate total price |
| FR6 | Remove item | Xoá cart item + toppings khỏi cart |
| FR7 | Change branch | Khi đổi branch → clear cart cũ, set branch mới |
| FR8 | Apply voucher | Validate và apply voucher vào cart, tính discount |
| FR9 | Remove voucher | Xoá voucher khỏi cart |
| FR10 | Cart summary | Tính subtotal, discount, total của cart |
| FR11 | Create order | Tạo order từ cart với đầy đủ snapshot + validation |
| FR12 | Get orders | Lấy danh sách order của user (phân trang) |
| FR13 | Get order detail | Lấy chi tiết order + items + toppings + status history |
| FR14 | Cancel order | Cancel order đang PENDING, refund voucher usage |

### 7.2 Non-Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| NFR1 | Consistency | `createOrder()` phải là transaction atomic: không thể tạo order nửa vời |
| NFR2 | Atomic voucher | `incrementUsageCount` / `decrementUsageCountByCode` phải atomic (lock row / update count) |
| NFR3 | Idempotency | Cart item update/remove nên idempotent |
| NFR4 | Performance | Cart operations < 200ms, order creation < 500ms |

---

## 8. Design / Giải pháp thiết kế

### 8.1 Kiến trúc tổng quan

```
┌──────────┐     ┌────────────────────────────────────────────────────────────────┐
│  Client   │     │                    Backend                                    │
│ (React)   │     │                                                                │
│           │     │  ┌──────────┐   ┌───────────────┐   ┌──────────────────────┐   │
│  cart     │────►│  │  Cart    │   │  CartService  │   │  CartRepository      │   │
│  APIs     │     │  │Controller│──►│  + Voucher    │──►│  VoucherRepository   │   │
│           │     │  └──────────┘   │  + Validation │   │  Branch/Availability │   │
│  order    │     │                 └───────────────┘   └──────────────────────┘   │
│  APIs     │────►│  ┌──────────┐   ┌───────────────┐   ┌──────────────────────┐   │
│           │     │  │  Order   │   │  OrderService │   │  OrderRepository     │   │
│           │     │  │Controller│──►│  + Status     │──►│  OrderItemRepos.    │   │
│           │     │  └──────────┘   │  + History    │   │  StatusHistoryRepos. │   │
└──────────┘     └────────────────────────────────────────────────────────────────┘
                                   │
                                   ▼
                          ┌──────────────────┐
                          │  VoucherService  │
                          │  - validate      │
                          │  - apply atomic  │
                          │  - refund        │
                          └──────────────────┘
```

### 8.2 Entity Relationships

```
Cart (1) ──── (N) CartItem (1) ──── (N) CartItemTopping
  ├── sessionId (guest)
  ├── customer (logged-in)
  └── branch

CartItem
  ├── product, productName
  ├── variant, variantName
  ├── quantity, unitPrice, totalPrice
  ├── sugarLevel, iceLevel
  └── note

CartItemTopping
  ├── topping, toppingName
  ├── quantity, unitPrice, totalPrice

─── Khi create order, snapshot được copy ───

Order (1) ──── (N) OrderItem (1) ──── (N) OrderItemTopping
  ├── orderCode (unique, generated)
  ├── customer, customerName/Phone/Email
  ├── branch
  ├── orderType (PICKUP/DELIVERY)
  ├── paymentStatus
  ├── subtotalAmount, discountAmount, deliveryFee, totalAmount
  ├── deliveryAddress, pickupTime
  └── timestamps (confirmedAt, preparedAt, readyAt, completedAt, cancelledAt)

OrderItem
  ├── product, productCode, productName
  ├── variant, variantName
  ├── quantity, unitPrice, totalPrice
  ├── sugarLevel, iceLevel
  └── note

OrderItemTopping
  ├── topping, toppingCode, toppingName
  ├── quantity, unitPrice, totalPrice

─── Status tracking ───

Order (1) ──── (N) OrderStatusHistory
  ├── oldStatus, newStatus
  ├── reason
  └── changedBy

─── Voucher ───

Voucher (1) ──── (N) VoucherBranch
Voucher (1) ──── (N) VoucherUsage
```

### 8.3 Data Flow

#### 8.3.1 Cart Flow

```
Guest request (cookie: sessionId)
  │
  ▼
CartController:
  1. Lấy sessionId từ Cookie/Header "X-Session-Id"
  2. Nếu có JWT → lấy customerId từ token
  3. Forward xuống CartService

CartService.findOrCreateCart(sessionId, customerId):
  ┌─ Logged-in (customerId != null):
  │   └─ CartRepository.findByCustomerIdAndStatus(customerId, ACTIVE)
  │       ├─ Found → return
  │       └─ Not found → tạo mới (set customer + branch null)
  │
  └─ Guest (sessionId != null):
      └─ CartRepository.findBySessionIdAndStatus(sessionId, ACTIVE)
          ├─ Found → return
          └─ Not found → tạo mới (set sessionId + branch null)

CartService.addItem(request, sessionId, customerId):
  1. findOrCreateCart → Cart
  2. Nếu cart branch null → set branch từ request
  3. Nếu khác branch → delete cart items cũ → set branch mới
  4. Validate BranchProductAvailability tại branch
  5. Validate variant (nếu có)
  6. Validate toppings exist (nếu có)
  7. Tính unitPrice = product.basePrice + variant.additionalPrice (nếu có)
  8. Tính totalPrice = (unitPrice + sum(topping.unitPrice)) * quantity
  9. Save CartItem + CartItemToppings
  10. recalculateCartTotal(cart)
  11. Return CartResponse
```

#### 8.3.2 Order Creation Flow

```
POST /api/v1/orders (Authenticated)
  │
  ▼
OrderController.createOrder(request, @CurrentUser)
  │
  ▼
OrderService.createOrder(request, customerProfile):
  1. Tìm Cart active của customer
  2. Validate cart không rỗng
  3. Validate branch hoạt động (status ACTIVE)
  4. Validate pickup time slot (nếu PICKUP)
  5. Validate delivery address (nếu DELIVERY)
  6. Tính subtotal = sum(cart.items.totalPrice)
  7. Tính deliveryFee dựa trên branch config
  8. Apply voucher (nếu có) → discount
  9. Tính total = subtotal - discount + deliveryFee
  10. Tạo Order → orderCode sinh từ CodeGenerator
  11. Copy CartItem → OrderItem (snapshot: productName, prices)
  12. Copy CartItemTopping → OrderItemTopping (snapshot: toppingName, prices)
  13. Tạo OrderStatusHistory: null → PENDING
  14. Clear cart (delete items + cart)
  15. Trả về OrderResponse
```

### 8.4 Sequence Diagrams

#### 8.4.1 Add Item to Cart

```
Client                    CartController             CartService              Repositories
  │                            │                        │                        │
  │── POST /cart/items ───────►│                        │                        │
  │   {productId, variantId,  │                        │                        │
  │    toppingIds, quantity,  │                        │                        │
  │    sugarLevel, iceLevel,  │                        │                        │
  │    branchId, note}        │                        │                        │
  │                            │── findOrCreateCart ──►│                        │
  │                            │                        │── findByCustomerId ──►│
  │                            │                        │◄── Cart ──────────────│
  │                            │                        │                        │
  │                            │◄── Cart ───────────────│                        │
  │                            │                        │                        │
  │                            │── addItem ────────────►│                        │
  │                            │                        │── findAvailability ───►│
  │                            │                        │◄── BranchProductAvail ─│
  │                            │                        │── findById(variant) ──►│
  │                            │                        │◄── Variant ───────────│
  │                            │                        │── findAllById(topping)►│
  │                            │                        │◄── List<Topping> ─────│
  │                            │                        │                        │
  │                            │                        │── save(item) ─────────►│
  │                            │                        │◄── CartItem ──────────│
  │                            │                        │── saveAll(toppings) ──►│
  │                            │                        │── update(cart.total) ─►│
  │                            │                        │                        │
  │                            │◄── CartResponse ───────│                        │
  │◄── BaseResponse.ok(data) ──│                        │                        │
  │                            │                        │                        │
```

#### 8.4.2 Create Order

```
Client                    OrderController              OrderService              CartService/VoucherService
  │                            │                          │                          │
  │── POST /orders ───────────►│                          │                          │
  │   {voucherCode,           │                          │                          │
  │    orderType,              │                          │                          │
  │    deliveryAddress,        │                          │                          │
  │    pickupTime,             │                          │                          │
  │    note}                   │                          │                          │
  │                            │── createOrder ──────────►│                          │
  │                            │                          │── findByCustomerId ─────►│ (CartRepo)
  │                            │                          │◄── Cart ────────────────│
  │                            │                          │                          │
  │                            │                          │── findById(branch) ─────►│ (BranchRepo)
  │                            │                          │◄── Branch ──────────────│
  │                            │                          │                          │
  │                            │                          │── validateAndApply ─────►│ (VoucherService)
  │                            │                          │◄── VoucherResult ───────│
  │                            │                          │                          │
  │                            │                          │── save(order) ──────────►│ (OrderRepo)
  │                            │                          │── saveAll(items) ───────►│ (OrderItemRepo)
  │                            │                          │── saveAll(toppings) ────►│ (OrderItemToppingRepo)
  │                            │                          │── save(history) ────────►│ (StatusHistoryRepo)
  │                            │                          │── delete(cart.items) ───►│ (CartItemRepo)
  │                            │                          │── delete(cart) ─────────►│ (CartRepo)
  │                            │                          │                          │
  │                            │◄── OrderResponse ────────│                          │
  │◄── BaseResponse.ok(data) ──│                          │                          │
  │                            │                          │                          │
```

#### 8.4.3 Cancel Order

```
Client                    OrderController              OrderService              VoucherService
  │                            │                          │                          │
  │── POST /orders/{id}/cancel─►│                          │                          │
  │                            │── cancelOrder ──────────►│                          │
  │                            │                          │── findById(id) ─────────►│
  │                            │                          │◄── Order ───────────────│
  │                            │                          │                          │
  │                            │                          │  Verify: order.customer  │
  │                            │                          │  == currentCustomer      │
  │                            │                          │  Verify: status PENDING  │
  │                            │                          │                          │
  │                            │                          │── setStatus(CANCELLED) ──►│
  │                            │                          │── save(order) ──────────►│
  │                            │                          │                          │
  │                            │                          │── decrementUsage ───────►│ (nếu có voucher)
  │                            │                          │◄── ok ──────────────────│
  │                            │                          │                          │
  │                            │                          │── save(history) ────────►│
  │                            │                          │                          │
  │                            │◄── void ─────────────────│                          │
  │◄── BaseResponse.ok() ──────│                          │                          │
  │                            │                          │                          │
```

### 8.5 API Design

#### 8.5.1 Cart Endpoints

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| `GET` | `/api/v1/cart` | Optional (JWT or sessionId) | Get current cart |
| `POST` | `/api/v1/cart/items` | Optional | Add item to cart |
| `PUT` | `/api/v1/cart/items/{itemId}` | Optional | Update cart item quantity/sugar/ice |
| `DELETE` | `/api/v1/cart/items/{itemId}` | Optional | Remove item from cart |
| `POST` | `/api/v1/cart/apply-voucher` | Optional | Apply voucher code |
| `DELETE` | `/api/v1/cart/apply-voucher` | Optional | Remove voucher |
| `GET` | `/api/v1/cart/summary` | Optional | Get cart summary (subtotal, discount, total) |

#### 8.5.2 Order Endpoints

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| `POST` | `/api/v1/orders` | Authenticated | Create order từ cart |
| `GET` | `/api/v1/orders` | Authenticated | Get user's orders (phân trang) |
| `GET` | `/api/v1/orders/{orderId}` | Authenticated | Get order detail |
| `POST` | `/api/v1/orders/{orderId}/cancel` | Authenticated | Cancel order (PENDING only) |

### 8.6 DTO Design

#### 8.6.1 Request DTOs

**AddCartItemRequest**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AddCartItemRequest {
    @NotNull private String branchId;
    @NotNull private String productId;
    private String variantId;
    private List<String> toppingIds;
    @Min(1) private int quantity = 1;
    private String sugarLevel = "NORMAL";  // SugarLevel enum value
    private String iceLevel = "NORMAL";     // IceLevel enum value
    private String note;
}
```

**UpdateCartItemRequest**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UpdateCartItemRequest {
    @Min(1) private Integer quantity;
    private String sugarLevel;
    private String iceLevel;
    private String note;
}
```

**ApplyVoucherRequest**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ApplyVoucherRequest {
    @NotBlank private String voucherCode;
}
```

**CreateOrderRequest**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateOrderRequest {
    private String voucherCode;
    @NotBlank private String orderType;  // PICKUP or DELIVERY
    private String deliveryAddress;     // required if DELIVERY
    private LocalDateTime pickupTime;   // required if PICKUP
    private String note;
}
```

#### 8.6.2 Response DTOs

**CartResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartResponse {
    private String id;
    private BranchSummary branch;
    private List<CartItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal total;
    private String appliedVoucherCode;
    private int itemCount;
}
```

**CartItemResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemResponse {
    private String id;
    private ProductSummary product;
    private VariantSummary variant;
    private List<CartItemToppingResponse> toppings;
    private int quantity;
    private String sugarLevel;
    private String iceLevel;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String note;
}
```

**CartItemToppingResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItemToppingResponse {
    private String id;
    private String toppingId;
    private String toppingName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
```

**OrderResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private String id;
    private String orderCode;
    private BranchSummary branch;
    private String customerName;
    private String orderType;
    private String status;
    private String paymentStatus;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal totalAmount;
    private String deliveryAddress;
    private LocalDateTime pickupTime;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;
    private List<OrderStatusHistoryResponse> statusHistory;
    private String note;
}
```

**OrderItemResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemResponse {
    private String id;
    private String productCode;
    private String productName;
    private String variantName;
    private int quantity;
    private String sugarLevel;
    private String iceLevel;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String note;
    private List<OrderItemToppingResponse> toppings;
}
```

**OrderItemToppingResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItemToppingResponse {
    private String id;
    private String toppingCode;
    private String toppingName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}
```

**OrderStatusHistoryResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistoryResponse {
    private String fromStatus;
    private String toStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
}
```

**CartSummaryResponse**
```java
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartSummaryResponse {
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal deliveryFee;
    private BigDecimal total;
    private String appliedVoucherCode;
}
```

**Shared Nested DTOs**
```java
// Dùng chung cho cart & order responses
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BranchSummary {
    private String id;
    private String code;
    private String name;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductSummary {
    private String id;
    private String code;
    private String name;
}

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VariantSummary {
    private String id;
    private String code;
    private String name;
}
```

### 8.7 Package Structure

```
com.hoandev.pinedrink
├── controller/
│   ├── CartController.java          // NEW
│   └── OrderController.java         // NEW
├── service/
│   ├── CartService.java             // NEW (interface)
│   ├── OrderService.java            // NEW (interface)
│   ├── VoucherService.java          // NEW (interface)
│   └── impl/
│       ├── CartServiceImpl.java     // NEW
│       ├── OrderServiceImpl.java    // NEW
│       └── VoucherServiceImpl.java  // NEW
├── mapper/
│   ├── CartMapper.java              // NEW
│   └── OrderMapper.java             // NEW
└── entity/dto/
    ├── request/
    │   ├── cart/
    │   │   ├── AddCartItemRequest.java
    │   │   ├── UpdateCartItemRequest.java
    │   │   └── ApplyVoucherRequest.java
    │   └── order/
    │       └── CreateOrderRequest.java
    └── response/
        ├── cart/
        │   ├── CartResponse.java
        │   ├── CartItemResponse.java
        │   ├── CartItemToppingResponse.java
        │   └── CartSummaryResponse.java
        ├── order/
        │   ├── OrderResponse.java
        │   ├── OrderItemResponse.java
        │   ├── OrderItemToppingResponse.java
        │   └── OrderStatusHistoryResponse.java
        └── shared/
            ├── BranchSummary.java
            ├── ProductSummary.java
            └── VariantSummary.java
```

### 8.8 Error Codes

Bổ sung vào `ErrorCode.java`:

```java
CART_001("CART_001", "Cart is empty"),
CART_002("CART_002", "Cart not found"),
CART_003("CART_003", "Cart item not found"),
CART_004("CART_004", "Product not available at this branch"),
CART_005("CART_005", "Product is currently unavailable at this branch"),
CART_006("CART_006", "Invalid variant for this product"),
CART_007("CART_007", "One or more toppings not found"),
CART_008("CART_008", "Cannot change branch with active items"),

ORDER_001("ORDER_001", "Order not found"),
ORDER_002("ORDER_002", "Branch is currently closed"),
ORDER_003("ORDER_003", "You can only cancel your own orders"),
ORDER_004("ORDER_004", "Can only cancel PENDING orders"),
ORDER_005("ORDER_005", "Invalid order type"),

VOUCHER_001("VOUCHER_001", "Invalid voucher code"),
VOUCHER_002("VOUCHER_002", "Voucher has expired or not yet active"),
VOUCHER_003("VOUCHER_003", "Voucher usage limit reached"),
VOUCHER_004("VOUCHER_004", "Minimum order amount not met"),
VOUCHER_005("VOUCHER_005", "Voucher not valid for this branch"),
```

---

## 9. Details / Chi tiết triển khai

### 9.1 CartMapper

**File**: `mapper/CartMapper.java`

```java
@Component
public class CartMapper {

    public CartResponse toResponse(Cart cart) {
        if (cart == null) return null;

        List<CartItemResponse> items = cart.getCartItems() != null
                ? cart.getCartItems().stream().map(this::toItemResponse).toList()
                : List.of();

        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal discount = calculateDiscount(cart);  // từ voucher
        int itemCount = items.stream().mapToInt(CartItemResponse::getQuantity).sum();

        return CartResponse.builder()
                .id(cart.getId())
                .branch(toBranchSummary(cart.getBranch()))
                .items(items)
                .subtotal(subtotal)
                .discount(discount)
                .total(subtotal.subtract(discount))
                .appliedVoucherCode(null)  // xử lý sau
                .itemCount(itemCount)
                .build();
    }

    private CartItemResponse toItemResponse(CartItem item) {
        List<CartItemToppingResponse> toppings = item.getCartItemToppings() != null
                ? item.getCartItemToppings().stream().map(this::toToppingResponse).toList()
                : List.of();

        return CartItemResponse.builder()
                .id(item.getId())
                .product(toProductSummary(item.getProduct()))
                .variant(toVariantSummary(item.getVariant()))
                .toppings(toppings)
                .quantity(item.getQuantity())
                .sugarLevel(item.getSugarLevel())
                .iceLevel(item.getIceLevel())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .note(item.getNote())
                .build();
    }

    private CartItemToppingResponse toToppingResponse(CartItemTopping topping) {
        return CartItemToppingResponse.builder()
                .id(topping.getId())
                .toppingId(topping.getTopping().getId())
                .toppingName(topping.getTopping().getName())
                .quantity(topping.getQuantity())
                .unitPrice(topping.getUnitPrice())
                .totalPrice(topping.getTotalPrice())
                .build();
    }

    private BranchSummary toBranchSummary(Branch branch) {
        if (branch == null) return null;
        return BranchSummary.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .build();
    }

    private ProductSummary toProductSummary(Product product) {
        if (product == null) return null;
        return ProductSummary.builder()
                .id(product.getId())
                .code(product.getCode())
                .name(product.getName())
                .build();
    }

    private VariantSummary toVariantSummary(ProductVariant variant) {
        if (variant == null) return null;
        return VariantSummary.builder()
                .id(variant.getId())
                .code(variant.getVariantCode())
                .name(variant.getName())
                .build();
    }

    public BigDecimal calculateSubtotal(Cart cart) {
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty())
            return BigDecimal.ZERO;
        return cart.getCartItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateDiscount(Cart cart) {
        // Placeholder — VoucherService sẽ set discount thực tế
        return BigDecimal.ZERO;
    }
}
```

### 9.2 OrderMapper

**File**: `mapper/OrderMapper.java`

```java
@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        if (order == null) return null;

        List<OrderItemResponse> items = order.getOrderItems() != null
                ? order.getOrderItems().stream().map(this::toItemResponse).toList()
                : List.of();

        List<OrderStatusHistoryResponse> history = order.getStatusHistories() != null
                ? order.getStatusHistories().stream().map(this::toHistoryResponse).toList()
                : List.of();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .branch(toBranchSummary(order.getBranch()))
                .customerName(order.getCustomerName())
                .orderType(order.getOrderType())
                .status(order.getStatus())
                .paymentStatus(order.getPaymentStatus())
                .subtotalAmount(order.getSubtotalAmount())
                .discountAmount(order.getDiscountAmount())
                .deliveryFee(order.getDeliveryFee())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .pickupTime(order.getPickupTime())
                .createdAt(order.getCreatedAt())
                .items(items)
                .statusHistory(history)
                .note(order.getNote())
                .build();
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        List<OrderItemToppingResponse> toppings = item.getOrderItemToppings() != null
                ? item.getOrderItemToppings().stream().map(this::toToppingResponse).toList()
                : List.of();

        return OrderItemResponse.builder()
                .id(item.getId())
                .productCode(item.getProductCode())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .quantity(item.getQuantity())
                .sugarLevel(item.getSugarLevel())
                .iceLevel(item.getIceLevel())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .note(item.getNote())
                .toppings(toppings)
                .build();
    }

    private OrderItemToppingResponse toToppingResponse(OrderItemTopping topping) {
        return OrderItemToppingResponse.builder()
                .id(topping.getId())
                .toppingCode(topping.getToppingCode())
                .toppingName(topping.getToppingName())
                .quantity(topping.getQuantity())
                .unitPrice(topping.getUnitPrice())
                .totalPrice(topping.getTotalPrice())
                .build();
    }

    private OrderStatusHistoryResponse toHistoryResponse(OrderStatusHistory history) {
        return OrderStatusHistoryResponse.builder()
                .fromStatus(history.getOldStatus())
                .toStatus(history.getNewStatus())
                .reason(history.getReason())
                .changedBy(history.getChangedBy() != null
                        ? history.getChangedBy().getUsername() : null)
                .changedAt(history.getChangedAt())
                .build();
    }

    private BranchSummary toBranchSummary(Branch branch) {
        if (branch == null) return null;
        return BranchSummary.builder()
                .id(branch.getId())
                .code(branch.getCode())
                .name(branch.getName())
                .build();
    }
}
```

### 9.3 CartService

**File**: `service/CartService.java`

```java
public interface CartService {
    CartResponse getCart(String sessionId, String customerId);
    CartResponse addItem(AddCartItemRequest request, String sessionId, String customerId);
    CartResponse updateItem(String itemId, UpdateCartItemRequest request, String sessionId, String customerId);
    void removeItem(String itemId, String sessionId, String customerId);
    CartResponse applyVoucher(ApplyVoucherRequest request, String sessionId, String customerId);
    CartResponse removeVoucher(String sessionId, String customerId);
    CartSummaryResponse getSummary(String sessionId, String customerId);
    void clearCart(String cartId);
}
```

**File**: `service/impl/CartServiceImpl.java`

Logic chính:

```java
@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemToppingRepository cartItemToppingRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ToppingRepository toppingRepository;
    private final BranchProductAvailabilityRepository availabilityRepo;
    private final VoucherRepository voucherRepository;
    private final CartMapper cartMapper;

    @Override
    public CartResponse getCart(String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse addItem(AddCartItemRequest request, String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);

        // Handle branch change
        if (cart.getBranch() == null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
            cart.setBranch(branch);
        } else if (!cart.getBranch().getId().equals(request.getBranchId())) {
            cartItemRepository.deleteByCartId(cart.getId());
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
            cart.setBranch(branch);
        }

        // Validate product availability at branch
        BranchProductAvailability availability = availabilityRepo
                .findByBranchIdAndProductId(cart.getBranch().getId(), request.getProductId())
                .orElseThrow(() -> new BaseException(ErrorCode.CART_004));
        if (!availability.isAvailable()) {
            throw new BaseException(ErrorCode.CART_005);
        }

        // Validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_001));

        // Validate variant
        ProductVariant variant = null;
        if (request.getVariantId() != null) {
            variant = variantRepository.findById(request.getVariantId())
                    .orElseThrow(() -> new BaseException(ErrorCode.PRODUCT_007));
        }

        // Validate toppings
        List<Topping> toppings = Collections.emptyList();
        if (request.getToppingIds() != null && !request.getToppingIds().isEmpty()) {
            toppings = toppingRepository.findAllById(request.getToppingIds());
            if (toppings.size() != request.getToppingIds().size()) {
                throw new BaseException(ErrorCode.CART_007);
            }
        }

        // Validate sugar/ice level
        validateSugarLevel(request.getSugarLevel());
        validateIceLevel(request.getIceLevel());

        // Calculate prices
        BigDecimal unitPrice = calculateUnitPrice(product, variant);
        BigDecimal toppingsTotal = calculateToppingsTotal(toppings);
        BigDecimal totalPrice = unitPrice.add(toppingsTotal)
                .multiply(BigDecimal.valueOf(request.getQuantity()));

        // Create CartItem
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProduct(product);
        item.setVariant(variant);
        item.setQuantity(request.getQuantity());
        item.setSugarLevel(request.getSugarLevel());
        item.setIceLevel(request.getIceLevel());
        item.setUnitPrice(unitPrice);
        item.setTotalPrice(totalPrice);
        item.setNote(request.getNote());
        item = cartItemRepository.save(item);

        // Save CartItemToppings
        if (!toppings.isEmpty()) {
            List<CartItemTopping> itemToppings = toppings.stream()
                    .map(t -> {
                        CartItemTopping cit = new CartItemTopping();
                        cit.setCartItem(item);
                        cit.setTopping(t);
                        cit.setQuantity(1);
                        cit.setUnitPrice(t.getPrice());
                        cit.setTotalPrice(t.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
                        return cit;
                    })
                    .toList();
            cartItemToppingRepository.saveAll(itemToppings);
        }

        // Reload cart with fresh data
        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse updateItem(String itemId, UpdateCartItemRequest request,
                                    String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BaseException(ErrorCode.CART_003));

        // Verify item belongs to this cart
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BaseException(ErrorCode.CART_003);
        }

        if (request.getQuantity() != null) {
            item.setQuantity(request.getQuantity());
        }
        if (request.getSugarLevel() != null) {
            validateSugarLevel(request.getSugarLevel());
            item.setSugarLevel(request.getSugarLevel());
        }
        if (request.getIceLevel() != null) {
            validateIceLevel(request.getIceLevel());
            item.setIceLevel(request.getIceLevel());
        }
        if (request.getNote() != null) {
            item.setNote(request.getNote());
        }

        // Recalculate: unitPrice giữ nguyên, totalPrice thay đổi theo quantity
        BigDecimal toppingsTotal = calculateToppingsTotal(
                item.getCartItemToppings().stream()
                        .map(CartItemTopping::getTopping)
                        .toList());
        BigDecimal newTotalPrice = item.getUnitPrice().add(toppingsTotal)
                .multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setTotalPrice(newTotalPrice);

        cartItemRepository.save(item);

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return cartMapper.toResponse(cart);
    }

    @Override
    public void removeItem(String itemId, String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new BaseException(ErrorCode.CART_003));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new BaseException(ErrorCode.CART_003);
        }

        cartItemRepository.delete(item);
    }

    @Override
    public CartResponse applyVoucher(ApplyVoucherRequest request,
                                      String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        BigDecimal subtotal = cartMapper.calculateSubtotal(cart);

        // Validate voucher — delegate to VoucherService
        VoucherValidationResult result = voucherService.validateAndApply(
                request.getVoucherCode(), subtotal, customerId, cart.getBranch().getId());

        // Store applied voucher on cart (transient — sẽ lưu xuống DB nếu cần persist)
        // Cart entity hiện không có voucher field — sẽ store trong service state hoặc
        // thêm transient field. Ở phase 1: chỉ validate + return discount trong response.

        cart = cartRepository.findById(cart.getId()).orElseThrow();
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartResponse removeVoucher(String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        // Clear applied voucher
        return cartMapper.toResponse(cart);
    }

    @Override
    public CartSummaryResponse getSummary(String sessionId, String customerId) {
        Cart cart = findOrCreateCart(sessionId, customerId);
        BigDecimal subtotal = cartMapper.calculateSubtotal(cart);
        return CartSummaryResponse.builder()
                .subtotal(subtotal)
                .discount(BigDecimal.ZERO)
                .deliveryFee(BigDecimal.ZERO)
                .total(subtotal)
                .build();
    }

    @Override
    public void clearCart(String cartId) {
        cartItemRepository.deleteByCartId(cartId);
        cartRepository.deleteById(cartId);
    }

    // ─── Private helpers ──────────────────────────────────────────

    private Cart findOrCreateCart(String sessionId, String customerId) {
        if (customerId != null) {
            return cartRepository.findByCustomerIdAndStatus(customerId, "ACTIVE")
                    .orElseGet(() -> {
                        Cart cart = new Cart();
                        // customer sẽ được set bởi service layer
                        return cartRepository.save(cart);
                    });
        } else if (sessionId != null) {
            return cartRepository.findBySessionIdAndStatus(sessionId, "ACTIVE")
                    .orElseGet(() -> {
                        Cart cart = new Cart();
                        cart.setSessionId(sessionId);
                        return cartRepository.save(cart);
                    });
        }
        throw new BaseException(ErrorCode.CART_002);
    }

    private BigDecimal calculateUnitPrice(Product product, ProductVariant variant) {
        BigDecimal base = product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO;
        if (variant != null && variant.getAdditionalPrice() != null) {
            base = base.add(variant.getAdditionalPrice());
        }
        return base;
    }

    private BigDecimal calculateToppingsTotal(List<Topping> toppings) {
        return toppings.stream()
                .map(t -> t.getPrice() != null ? t.getPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateSugarLevel(String level) {
        if (level == null) return;
        try {
            SugarLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.COM_001);
        }
    }

    private void validateIceLevel(String level) {
        if (level == null) return;
        try {
            IceLevel.valueOf(level);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCode.COM_001);
        }
    }
}
```

### 9.4 VoucherService

**File**: `service/VoucherService.java`

```java
public interface VoucherService {
    VoucherValidationResult validateAndApply(String code, BigDecimal subtotal,
                                              String customerId, String branchId);
    void decrementUsageCount(String voucherCode);
}
```

**File**: `service/impl/VoucherServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherBranchRepository voucherBranchRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    public VoucherValidationResult validateAndApply(String code, BigDecimal subtotal,
                                                     String customerId, String branchId) {
        Voucher voucher = voucherRepository.findByCode(code)
                .orElseThrow(() -> new BaseException(ErrorCode.VOUCHER_001));

        // Check active status
        if (!"ACTIVE".equals(voucher.getStatus())) {
            throw new BaseException(ErrorCode.VOUCHER_001);
        }

        // Check date range
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getStartAt()) || now.isAfter(voucher.getEndAt())) {
            throw new BaseException(ErrorCode.VOUCHER_002);
        }

        // Check global usage limit
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new BaseException(ErrorCode.VOUCHER_003);
        }

        // Check per-customer usage limit
        if (voucher.getUsageLimitPerCustomer() != null && customerId != null) {
            long customerUsage = voucherUsageRepository
                    .countByCustomerIdAndVoucherId(customerId, voucher.getId());
            if (customerUsage >= voucher.getUsageLimitPerCustomer()) {
                throw new BaseException(ErrorCode.VOUCHER_003);
            }
        }

        // Check minimum order amount
        if (subtotal.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new BaseException(ErrorCode.VOUCHER_004);
        }

        // Check branch scope
        // Voucher entity không có scope field — dùng VoucherBranch để check
        boolean hasBranchRestrictions = voucherBranchRepository.existsByVoucherId(voucher.getId());
        if (hasBranchRestrictions) {
            boolean validForBranch = voucherBranchRepository
                    .existsByVoucherIdAndBranchId(voucher.getId(), branchId);
            if (!validForBranch) {
                throw new BaseException(ErrorCode.VOUCHER_005);
            }
        }

        // Calculate discount
        BigDecimal discountAmount;
        if (DiscountType.PERCENTAGE.getValue().equals(voucher.getDiscountType())) {
            discountAmount = subtotal.multiply(
                    voucher.getDiscountValue().divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            if (voucher.getMaxDiscountAmount() != null &&
                    discountAmount.compareTo(voucher.getMaxDiscountAmount()) > 0) {
                discountAmount = voucher.getMaxDiscountAmount();
            }
        } else {
            discountAmount = voucher.getDiscountValue(); // FIXED_AMOUNT
        }

        // Atomic increment usage count — query-level update để tránh race condition
        voucherRepository.incrementUsageCount(voucher.getId());

        return VoucherValidationResult.builder()
                .voucherId(voucher.getId())
                .discountAmount(discountAmount)
                .build();
    }

    @Override
    @Transactional
    public void decrementUsageCount(String voucherCode) {
        voucherRepository.findByCode(voucherCode).ifPresent(voucher -> {
            if (voucher.getUsedCount() > 0) {
                voucher.setUsedCount(voucher.getUsedCount() - 1);
                voucherRepository.save(voucher);
            }
        });
    }
}
```

### 9.5 OrderService

**File**: `service/OrderService.java`

```java
public interface OrderService {
    OrderResponse createOrder(CreateOrderRequest request, String customerId);
    PageResponse<OrderResponse> getUserOrders(String customerId, Pageable pageable);
    OrderResponse getOrderDetail(String orderId, String customerId);
    void cancelOrder(String orderId, String customerId);
}
```

**File**: `service/impl/OrderServiceImpl.java`

```java
@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CartItemToppingRepository cartItemToppingRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemToppingRepository orderItemToppingRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final BranchRepository branchRepository;
    private final VoucherService voucherService;
    private final CodeGenerator codeGenerator;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request, String customerId) {
        // 1. Find active cart
        Cart cart = cartRepository.findByCustomerIdAndStatus(customerId, "ACTIVE")
                .orElseThrow(() -> new BaseException(ErrorCode.CART_001));

        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new BaseException(ErrorCode.CART_001);
        }

        // 2. Validate branch
        Branch branch = cart.getBranch();
        if (branch == null || !"ACTIVE".equals(branch.getStatus())) {
            throw new BaseException(ErrorCode.BRANCH_001);
        }

        // 3. Validate order type
        String orderType = request.getOrderType();
        boolean isPickup = OrderType.PICKUP.getValue().equals(orderType);
        boolean isDelivery = OrderType.DELIVERY.getValue().equals(orderType);

        if (!isPickup && !isDelivery) {
            throw new BaseException(ErrorCode.ORDER_005);
        }

        if (isDelivery && (request.getDeliveryAddress() == null || request.getDeliveryAddress().isBlank())) {
            throw new BaseException(ErrorCode.COM_001);
        }

        // 4. Calculate
        BigDecimal subtotal = calculateSubtotal(cart);
        BigDecimal deliveryFee = isDelivery ? calculateDeliveryFee(branch) : BigDecimal.ZERO;
        BigDecimal discount = BigDecimal.ZERO;
        String voucherCode = null;

        // 5. Apply voucher (nếu có)
        if (request.getVoucherCode() != null && !request.getVoucherCode().isBlank()) {
            VoucherValidationResult result = voucherService.validateAndApply(
                    request.getVoucherCode(), subtotal, customerId, branch.getId());
            discount = result.getDiscountAmount();
            voucherCode = request.getVoucherCode();
        }

        BigDecimal total = subtotal.subtract(discount).add(deliveryFee);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        // 6. Create Order
        Order order = new Order();
        order.setOrderCode(codeGenerator.generate("ORD", "ORDER"));
        order.setCustomer(cart.getCustomer());
        order.setCustomerName(cart.getCustomer().getFullName());
        order.setCustomerPhone(cart.getCustomer().getPhone());
        order.setCustomerEmail(cart.getCustomer().getEmail());
        order.setBranch(branch);
        order.setOrderType(orderType);
        order.setPaymentStatus(PaymentStatus.UNPAID.getValue());
        order.setSubtotalAmount(subtotal);
        order.setDiscountAmount(discount);
        order.setDeliveryFee(deliveryFee);
        order.setTotalAmount(total);
        order.setDeliveryAddress(isDelivery ? request.getDeliveryAddress() : null);
        order.setPickupTime(isPickup ? request.getPickupTime() : null);
        order.setNote(request.getNote());
        order = orderRepository.save(order);

        // 7. Copy CartItem → OrderItem
        for (CartItem cartItem : cart.getCartItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setProductCode(cartItem.getProduct().getCode());
            orderItem.setProductName(cartItem.getProduct().getName());
            orderItem.setVariant(cartItem.getVariant());
            orderItem.setVariantName(cartItem.getVariant() != null
                    ? cartItem.getVariant().getName() : null);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSugarLevel(cartItem.getSugarLevel());
            orderItem.setIceLevel(cartItem.getIceLevel());
            orderItem.setUnitPrice(cartItem.getUnitPrice());
            orderItem.setTotalPrice(cartItem.getTotalPrice());
            orderItem.setNote(cartItem.getNote());
            orderItem = orderItemRepository.save(orderItem);

            // Copy toppings
            if (cartItem.getCartItemToppings() != null) {
                for (CartItemTopping cartTopping : cartItem.getCartItemToppings()) {
                    OrderItemTopping orderTopping = new OrderItemTopping();
                    orderTopping.setOrderItem(orderItem);
                    orderTopping.setTopping(cartTopping.getTopping());
                    orderTopping.setToppingCode(cartTopping.getTopping().getCode());
                    orderTopping.setToppingName(cartTopping.getTopping().getName());
                    orderTopping.setQuantity(cartTopping.getQuantity());
                    orderTopping.setUnitPrice(cartTopping.getUnitPrice());
                    orderTopping.setTotalPrice(cartTopping.getTotalPrice());
                    orderItemToppingRepository.save(orderTopping);
                }
            }
        }

        // 8. Status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(null);
        history.setNewStatus(OrderStatus.PENDING.getValue());
        history.setReason("Order created");
        // changedBy có thể set sau
        statusHistoryRepository.save(history);

        // Also set order.status
        order.setStatus(OrderStatus.PENDING.getValue());
        orderRepository.save(order);

        // 9. Clear cart
        cartItemRepository.deleteByCartId(cart.getId());
        cartRepository.delete(cart);

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(String customerId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, pageable);
        List<OrderResponse> items = orders.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();
        return PageResponse.of(items, orders.getTotalElements(),
                orders.getNumber(), orders.getSize());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderDetail(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        // Verify ownership
        if (order.getCustomer() == null
                || !order.getCustomer().getId().equals(customerId)) {
            throw new BaseException(ErrorCode.ORDER_001);
        }

        return orderMapper.toResponse(order);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId, String customerId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BaseException(ErrorCode.ORDER_001));

        if (order.getCustomer() == null
                || !order.getCustomer().getId().equals(customerId)) {
            throw new BaseException(ErrorCode.ORDER_003);
        }

        if (!OrderStatus.PENDING.getValue().equals(order.getStatus())) {
            throw new BaseException(ErrorCode.ORDER_004);
        }

        order.setStatus(OrderStatus.CANCELLED.getValue());
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason("Customer cancelled");
        orderRepository.save(order);

        // Refund voucher
        if (order.getOrderCode() != null) {
            // Lấy voucher code từ order — cần thêm field voucher_code vào Order entity
            // hoặc query VoucherUsage
        }

        // Status history
        OrderStatusHistory history = new OrderStatusHistory();
        history.setOrder(order);
        history.setOldStatus(OrderStatus.PENDING.getValue());
        history.setNewStatus(OrderStatus.CANCELLED.getValue());
        history.setReason("Customer cancelled");
        statusHistoryRepository.save(history);
    }

    // ─── Helpers ──────────────────────────────────────────

    private BigDecimal calculateSubtotal(Cart cart) {
        return cart.getCartItems().stream()
                .map(CartItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateDeliveryFee(Branch branch) {
        // Phase 1: flat fee hoặc từ branch config
        // Có thể load từ ce_setting
        return BigDecimal.valueOf(10000); // 10,000 VND mặc định
    }
}
```

### 9.6 CartController

**File**: `controller/CartController.java`

```java
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Lấy sessionId từ header "X-Session-Id" hoặc cookie "session_id"
    // customerId từ @CurrentUser nếu đã login

    @GetMapping
    public BaseResponse<CartResponse> getCart(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.getCart(sessionId, customerId));
    }

    @PostMapping("/items")
    public BaseResponse<CartResponse> addItem(
            @Valid @RequestBody AddCartItemRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.addItem(request, sessionId, customerId));
    }

    @PutMapping("/items/{itemId}")
    public BaseResponse<CartResponse> updateItem(
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.updateItem(itemId, request, sessionId, customerId));
    }

    @DeleteMapping("/items/{itemId}")
    public BaseResponse<Void> removeItem(
            @PathVariable String itemId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        cartService.removeItem(itemId, sessionId, customerId);
        return BaseResponse.success(null, "Item removed from cart");
    }

    @PostMapping("/apply-voucher")
    public BaseResponse<CartResponse> applyVoucher(
            @Valid @RequestBody ApplyVoucherRequest request,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.applyVoucher(request, sessionId, customerId));
    }

    @DeleteMapping("/apply-voucher")
    public BaseResponse<CartResponse> removeVoucher(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.removeVoucher(sessionId, customerId));
    }

    @GetMapping("/summary")
    public BaseResponse<CartSummaryResponse> getSummary(
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @CurrentUser(required = false) UserPrincipal user) {
        String customerId = user != null ? user.getId() : null;
        return BaseResponse.success(cartService.getSummary(sessionId, customerId));
    }
}
```

### 9.7 OrderController

**File**: `controller/OrderController.java`

```java
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @RateLimit(key = "order", capacity = 20, refillTokens = 20, refillSeconds = 60)
    public BaseResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @CurrentUser UserPrincipal user) {
        return BaseResponse.success(
                orderService.createOrder(request, user.getId()),
                "Order created successfully");
    }

    @GetMapping
    public BaseResponse<PageResponse<OrderResponse>> getOrders(
            @CurrentUser UserPrincipal user,
            Pageable pageable) {
        return BaseResponse.success(
                orderService.getUserOrders(user.getId(), pageable));
    }

    @GetMapping("/{orderId}")
    public BaseResponse<OrderResponse> getOrderDetail(
            @PathVariable String orderId,
            @CurrentUser UserPrincipal user) {
        return BaseResponse.success(
                orderService.getOrderDetail(orderId, user.getId()));
    }

    @PostMapping("/{orderId}/cancel")
    public BaseResponse<Void> cancelOrder(
            @PathVariable String orderId,
            @CurrentUser UserPrincipal user) {
        orderService.cancelOrder(orderId, user.getId());
        return BaseResponse.success(null, "Order cancelled successfully");
    }
}
```

### 9.8 VoucherValidationResult DTO

```java
// Nằm trong service layer, không phải entity/dto
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VoucherValidationResult {
    private String voucherId;
    private BigDecimal discountAmount;
}
```

### 9.9 OrderRepository bổ sung

Cần thêm method cho phân trang:

```java
// Trong OrderRepository.java
Page<Order> findByCustomerIdOrderByCreatedAtDesc(String customerId, Pageable pageable);
```

Và cần thêm các repository method atomic cho voucher:

```java
// Trong VoucherRepository.java
@Modifying
@Query("UPDATE Voucher v SET v.usedCount = v.usedCount + 1 WHERE v.id = :id")
int incrementUsageCount(@Param("id") String id);

// Trong VoucherUsageRepository.java
long countByCustomerIdAndVoucherId(String customerId, String voucherId);
boolean existsByVoucherIdAndBranchId(String voucherId, String branchId);
boolean existsByVoucherId(String voucherId);  // có ràng buộc branch không
```

### 9.10 Security

Cart endpoints:
- `GET /api/v1/cart` — optional auth
- `POST /api/v1/cart/items` — optional auth
- `PUT /api/v1/cart/items/{itemId}` — optional auth
- `DELETE /api/v1/cart/items/{itemId}` — optional auth
- `POST /api/v1/cart/apply-voucher` — optional auth
- `DELETE /api/v1/cart/apply-voucher` — optional auth
- `GET /api/v1/cart/summary` — optional auth

Order endpoints (yêu cầu authenticated):
- `POST /api/v1/orders` — authenticated
- `GET /api/v1/orders` — authenticated
- `GET /api/v1/orders/{orderId}` — authenticated
- `POST /api/v1/orders/{orderId}/cancel` — authenticated

Cart security: mỗi user chỉ có thể thao tác với cart của chính mình.
Guest cart được bảo vệ bởi sessionId (tính như secret key ngắn hạn).

---

## 10. Acceptance Criteria

1. Guest có thể thêm sản phẩm vào cart với sessionId (không cần JWT)
2. Logged-in user có cart gắn với customerId
3. Add item validate product availability tại branch
4. Add item validate variant và toppings
5. Update item quantity → recalculate totalPrice
6. Remove item → xoá item + toppings
7. Apply voucher → validate + tính discount
8. Create order → snapshot cart thành order + items + toppings + status history
9. Create order → clear cart sau khi thành công
10. Cancel PENDING order → thành công
11. Cancel CONFIRMED/PREPARING/... order → thất bại với message "Can only cancel PENDING orders"
12. Voucher: check hạn sử dụng, usage limit, min order, branch scope
13. Voucher usedCount được increment khi apply
14. Voucher usedCount được decrement khi cancel order
15. API response format consistent: `BaseResponse<T>` với success/message/data/timestamp
16. Compile passes với `./mvnw -q -DskipTests compile`

---

## 11. Checklist

- [ ] Tạo ErrorCode mới: CART_001 → CART_008, ORDER_001 → ORDER_005, VOUCHER_001 → VOUCHER_005
- [ ] Tạo request DTOs: `AddCartItemRequest`, `UpdateCartItemRequest`, `ApplyVoucherRequest`, `CreateOrderRequest`
- [ ] Tạo response DTOs: `CartResponse`, `CartItemResponse`, `CartItemToppingResponse`, `CartSummaryResponse`
- [ ] Tạo response DTOs: `OrderResponse`, `OrderItemResponse`, `OrderItemToppingResponse`, `OrderStatusHistoryResponse`
- [ ] Tạo shared DTOs: `BranchSummary`, `ProductSummary`, `VariantSummary`
- [ ] Tạo `CartMapper.java` với toResponse + helpers
- [ ] Tạo `OrderMapper.java` với toResponse + helpers
- [ ] Tạo `VoucherValidationResult.java` service DTO
- [ ] Tạo `CartService.java` interface + `CartServiceImpl.java`
- [ ] Tạo `VoucherService.java` interface + `VoucherServiceImpl.java`
- [ ] Tạo `OrderService.java` interface + `OrderServiceImpl.java`
- [ ] Bổ sung `incrementUsageCount` query vào `VoucherRepository.java`
- [ ] Bổ sung `existsByVoucherIdAndBranchId` vào `VoucherBranchRepository.java`
- [ ] Bổ sung `countByCustomerIdAndVoucherId` vào `VoucherUsageRepository.java`
- [ ] Bổ sung `findByCustomerIdOrderByCreatedAtDesc(..., Pageable)` vào `OrderRepository.java`
- [ ] Tạo `CartController.java` với 7 endpoints
- [ ] Tạo `OrderController.java` với 4 endpoints
- [ ] Tạo `deleteByCartId` trong `CartItemRepository.java` (nếu chưa có)
- [ ] Verify compile với `./mvnw -q -DskipTests compile`
