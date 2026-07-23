# Plan 7 — Customer Features & Loyalty

## Mục tiêu
Xây dựng Customer profile management và Loyalty program với các tier, earning/redeeming points, và auto tier upgrade.

## Files cần tạo

| File | Mô tả |
|------|-------|
| `service/CustomerService.java` | Interface CustomerService |
| `service/impl/CustomerServiceImpl.java` | Implementation CustomerService |
| `service/LoyaltyService.java` | Interface LoyaltyService |
| `service/impl/LoyaltyServiceImpl.java` | Implementation LoyaltyService |
| `controller/CustomerController.java` | REST controller cho customer endpoints |

## Chi tiết implementation

### 1. Customer Features

- **Register**: Khi tạo account → auto create customer profile
- **Update profile**: name, phone, email, date_of_birth, gender
- **Address management**: CRUD shipping addresses, set default
- **Order history**: Get orders by customerId, filter by status, date range

### 2. Loyalty Program — Entity Models

```java
@Entity
@Table(name = "loyalty_account")
public class LoyaltyAccount {
    @Id
    private UUID id;
    
    @OneToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private LoyaltyTier tier; // SILVER, GOLD, PLATINUM, DIAMOND

    private Integer pointsBalance;     // điểm hiện tại
    private Integer lifetimePoints;    // tổng điểm tích lũy
}

public enum LoyaltyTier {
    SILVER(0, 99, 1.0),
    GOLD(100, 499, 1.2),
    PLATINUM(500, 999, 1.5),
    DIAMOND(1000, Integer.MAX_VALUE, 2.0);

    public final int minPoints;
    public final int maxPoints;
    public final double multiplier; // hệ số nhân khi earn points

    LoyaltyTier(int min, int max, double mult) {
        this.minPoints = min;
        this.maxPoints = max;
        this.multiplier = mult;
    }

    public static LoyaltyTier fromLifetimePoints(int points) {
        for (LoyaltyTier t : values()) {
            if (points >= t.minPoints && points <= t.maxPoints) return t;
        }
        return SILVER;
    }
}
```

### 3. LoyaltyService — Earn Points

```java
@Service
@RequiredArgsConstructor
public class LoyaltyServiceImpl implements LoyaltyService {

    private final LoyaltyAccountRepository accountRepository;
    private final PointTransactionRepository pointTransactionRepository;

    @Override
    @Transactional
    public void earnPoints(Order completedOrder) {
        LoyaltyAccount account = accountRepository
            .findByCustomerId(completedOrder.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Loyalty account not found"));

        int points = (int) (completedOrder.getTotalAmount() * account.getTier().multiplier);

        account.setPointsBalance(account.getPointsBalance() + points);
        account.setLifetimePoints(account.getLifetimePoints() + points);

        // Auto tier upgrade
        LoyaltyTier newTier = LoyaltyTier.fromLifetimePoints(account.getLifetimePoints());
        account.setTier(newTier);

        accountRepository.save(account);

        // Ghi point history
        PointTransaction tx = PointTransaction.builder()
                .accountId(account.getId())
                .orderId(completedOrder.getId())
                .type(TransactionType.EARN)
                .points(points)
                .balanceAfter(account.getPointsBalance())
                .description("Earned from order " + completedOrder.getOrderCode())
                .build();
        pointTransactionRepository.save(tx);
    }

    @Override
    @Transactional
    public void redeemPoints(UUID customerId, int points, BigDecimal discountAmount) {
        LoyaltyAccount account = accountRepository
            .findByCustomerId(customerId)
            .orElseThrow();

        if (account.getPointsBalance() < points) {
            throw new BadRequestException("Insufficient points");
        }

        account.setPointsBalance(account.getPointsBalance() - points);
        accountRepository.save(account);

        PointTransaction tx = PointTransaction.builder()
                .accountId(account.getId())
                .type(TransactionType.REDEEM)
                .points(-points)
                .balanceAfter(account.getPointsBalance())
                .description("Redeemed " + points + " points for " + discountAmount + " VND")
                .build();
        pointTransactionRepository.save(tx);
    }
}
```

### 4. CustomerController — Get Order History

```java
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final LoyaltyService loyaltyService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            @AuthenticationPrincipal UUID customerId) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.getProfile(customerId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> updateProfile(
            @AuthenticationPrincipal UUID customerId,
            @RequestBody @Valid UpdateProfileRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.updateProfile(customerId, request)));
    }

    @GetMapping("/addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal UUID customerId) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.getAddresses(customerId)));
    }

    @PostMapping("/addresses")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal UUID customerId,
            @RequestBody @Valid CreateAddressRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.createAddress(customerId, request)));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal UUID customerId,
            @PathVariable UUID id,
            @RequestBody @Valid UpdateAddressRequest request) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.updateAddress(customerId, id, request)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrderHistory(
            @AuthenticationPrincipal UUID customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(
            ApiResponse.success(customerService.getOrderHistory(customerId, status, fromDate, toDate)));
    }

    @GetMapping("/loyalty")
    public ResponseEntity<ApiResponse<LoyaltyAccountResponse>> getLoyalty(
            @AuthenticationPrincipal UUID customerId) {
        return ResponseEntity.ok(
            ApiResponse.success(loyaltyService.getAccount(customerId)));
    }

    @GetMapping("/loyalty/history")
    public ResponseEntity<ApiResponse<List<PointTransactionResponse>>> getLoyaltyHistory(
            @AuthenticationPrincipal UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            ApiResponse.success(loyaltyService.getTransactionHistory(customerId, page, size)));
    }
}
```

### 5. Loyalty Tiers

| Tier | Lifetime Points | Multiplier | Benefits |
|-----|----------------|-----------|----------|
| SILVER | 0-99 | x1.0 | Basic |
| GOLD | 100-499 | x1.2 | 20% bonus points |
| PLATINUM | 500-999 | x1.5 | 50% bonus points |
| DIAMOND | 1000+ | x2.0 | 100% bonus points |

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/customers/profile` | Xem profile |
| PUT | `/customers/profile` | Cập nhật profile |
| GET | `/customers/addresses` | Danh sách địa chỉ |
| POST | `/customers/addresses` | Thêm địa chỉ mới |
| PUT | `/customers/addresses/{id}` | Sửa địa chỉ |
| DELETE | `/customers/addresses/{id}` | Xóa địa chỉ |
| GET | `/customers/orders?status=&fromDate=&toDate=` | Lịch sử đơn hàng |
| GET | `/customers/loyalty` | Thông tin loyalty account |
| GET | `/customers/loyalty/history` | Lịch sử giao dịch điểm |

## Checklist

- [ ] Tạo Customer entity + Customer profile fields
- [ ] Tạo Address entity với CRUD + set default
- [ ] Tạo LoyaltyAccount entity + LoyaltyTier enum
- [ ] Tạo PointTransaction entity
- [ ] Tạo CustomerService interface + impl
- [ ] Tạo LoyaltyService interface + impl
- [ ] Auto-create loyalty account khi customer register
- [ ] Implement earnPoints khi order completed
- [ ] Implement redeemPoints (100 điểm = 10,000đ)
- [ ] Auto tier upgrade khi lifetime_points vượt ngưỡng
- [ ] Tạo CustomerController với đầy đủ endpoints
- [ ] Ghi point history cho mọi giao dịch
