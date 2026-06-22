# Thiết Kỹ Thuật - Dịch Vụ Phí Vận Chuyển

| Trường | Giá Trị |
|---|---|
| Mã Tài Liệu | TDD-PINE-ORDER-DELIVERY-FEE-001 |
| Dự Án | Pine Drink |
| Module | Đơn Hàng / Phí Vận Chuyển |
| Phiên Bản | 1.0 |
| Trạng Thái | Nháp |
| Cập Nhật Lần Cuối | 2026-06-18 |

---

## 1. Tổng Quan

Tính toán phí vận chuyển được tách khỏi `OrderService` sang `DeliveryFeeService`.

Thiết kế hiện tại không gọi API bản đồ/geocoding bên ngoài. Nó sử dụng tọa độ có sẵn từ:

- `Branch.latitude`, `Branch.longitude`
- Địa chỉ mặc định của khách hàng: `CustomerAddress.latitude`, `CustomerAddress.longitude`

Điều này giúp quá trình thanh toán nhanh chóng, có thể dự đoán được và không gặp vấn đề về giới hạn lượt gọi/phụ thuộc vào bên thứ ba.

---

## 2. Mục Tiêu

- Giữ logic tính phí vận chuyển bên ngoài `OrderService`.
- Sử dụng địa chỉ giao hàng mặc định của khách hàng từ hồ sơ.
- Xác thực địa chỉ và tọa độ bắt buộc trước khi tạo đơn giao hàng.
- Tính phí vận chuyển dựa trên khoảng cách tính bằng km.
- Giữ quy tắc giá có thể cấu hình thông qua `application.yaml`.
- Cho phép thay thế trong tương lai bằng tính năng định tuyến/tính khoảng cách thực mà không thay đổi `OrderService`.

---

## 3. Không Phục Mục Tiêu

- Không geocoding trong quá trình thanh toán.
- Không gọi API Ma Trận Khoảng Cách bên ngoài.
- Không có logic giao hàng cho người giao hàng.
- Không có quy trình theo dõi giao hàng.
- Chưa lưu khoảng cách đã tính trong `od_order`.

---

## 4. Triển Khai Hiện Tại

### 4.1 Các File Chính

| File | Mục Đích |
|---|---|
| `src/main/java/com/hoandev/pinedrink/service/DeliveryFeeService.java` | Hợp đồng dịch vụ |
| `src/main/java/com/hoandev/pinedrink/service/impl/DeliveryFeeServiceImpl.java` | Tính phí dựa trên Haversine |
| `src/main/java/com/hoandev/pinedrink/configuration/OrderProperties.java` | Ràng buộc cấu hình giao hàng |
| `src/main/resources/application.yaml` | Giá trị cấu hình runtime |
| `src/main/java/com/hoandev/pinedrink/repository/CustomerAddressRepository.java` | Truy vấn địa chỉ mặc định |
| `src/main/java/com/hoandev/pinedrink/service/impl/OrderServiceImpl.java` | Gọi `DeliveryFeeService` trong quá trình tạo đơn |

### 4.2 Hợp Đồng Dịch Vụ

```java
public interface DeliveryFeeService {
    BigDecimal calculate(Branch branch, CustomerAddress address, BigDecimal subtotal);
}
```

Dịch vụ chỉ trả về phí vận chuyển. Khoảng cách hiện là thông tin nội bộ.

---

## 5. Quy Trình Tạo Đơn Hàng

```text
POST /api/v1/orders
-> OrderService.createOrder(customerId, request)
-> xác thực khách hàng
-> xác thực chi nhánh
-> khóa giỏ hàng đang hoạt động
-> tính tổng phụ
-> nếu orderType == DELIVERY:
   -> lấy địa chỉ mặc định của khách hàng
   -> xác thực tọa độ chi nhánh
   -> xác thực tọa độ địa chỉ
   -> DeliveryFeeService.calculate(branch, address, subtotal)
   -> đặt order.deliveryAddress snapshot
   -> đặt order.deliveryFee
-> áp dụng giảm giá voucher
-> total = subtotal + deliveryFee - discount
-> lưu đơn hàng
-> sao chép mục giỏ hàng
-> dự trữ tồn kho
-> xóa giỏ hàng
```

---

## 6. Quy Tắc Chọn Địa Chỉ

Đơn giao hàng chỉ sử dụng địa chỉ mặc định của khách hàng.

Phương thức Repository:

```java
Optional<CustomerAddress> findByCustomerIdAndIsDefaultTrue(String customerId);
```

Quy tắc:

- Nếu không có địa chỉ mặc định -> từ chối đơn hàng.
- Nếu địa chỉ mặc định không có `latitude` hoặc `longitude` -> từ chối đơn hàng.
- `deliveryAddressId` từ request không được sử dụng trong quy trình hiện tại.

Lý Do:

- Hồ sơ khách hàng đã sở hữu dữ liệu địa chỉ.
- Tọa độ được kỳ vọng đã được lưu trước khi thanh toán.
- Quá trình thanh toán không nên phụ thuộc vào phân tích văn bản địa chỉ.

---

## 7. Công Thức Tính Phí

### 7.1 Khoảng Cách

Triển khai sử dụng công thức Haversine:

```text
straightDistanceKm = haversine(branch.lat, branch.lng, address.lat, address.lng)
estimatedRoadKm = straightDistanceKm * roadFactor
```

`roadFactor` xấp xỉ khoảng cách đường vì Haversine trả về khoảng cách đường thẳng.

Giá trị khuyến nghị:

```text
1.20 - 1.35
```

Mặc định hiện tại:

```text
1.25
```

### 7.2 Phí

```text
deliveryFee = baseFee + estimatedRoadKm * feePerKm
```

Làm tròn:

```text
scale 0, HALF_UP
```

### 7.3 Miễn Phí Vận Chuyển

```text
if subtotal >= freeThreshold:
    deliveryFee = 0
```

### 7.4 Khoảng Cách Tối Đa

```text
if estimatedRoadKm > maxDistanceKm:
    từ chối đơn hàng
```

---

## 8. Cấu Hình

Cấu hình hiện tại:

```yaml
order:
  delivery:
    default-fee: ${ORDER_DELIVERY_DEFAULT_FEE:15000}
    base-fee: ${ORDER_DELIVERY_BASE_FEE:10000}
    fee-per-km: ${ORDER_DELIVERY_FEE_PER_KM:5000}
    max-distance-km: ${ORDER_DELIVERY_MAX_DISTANCE_KM:15}
    road-factor: ${ORDER_DELIVERY_ROAD_FACTOR:1.25}
    free-threshold: ${ORDER_DELIVERY_FREE_THRESHOLD:200000}
```

| Thuộc Tính | Ý Nghĩa | Mặc Định |
|---|---|---|
| `default-fee` | Phí mặc định/cũ, hiện giữ lại để tương thích | `15000` |
| `base-fee` | Phí vận chuyển cố định | `10000` |
| `fee-per-km` | Phí trên mỗi km đường ước tính | `5000` |
| `max-distance-km` | Khoảng cách giao hàng tối đa cho phép | `15` |
| `road-factor` | Hệ số chuyển đổi từ khoảng cách thẳng sang khoảng cách đường ước tính | `1.25` |
| `free-threshold` | Tổng phụ đạt ngưỡng để miễn phí vận chuyển | `200000` |

---

## 9. Ví Dụ

Chi nhánh:

```text
latitude = 10.7769
longitude = 106.7009
```

Địa chỉ mặc định của khách hàng:

```text
latitude = 10.7547
longitude = 106.6638
```

Cấu hình:

```text
baseFee = 10000
feePerKm = 5000
roadFactor = 1.25
freeThreshold = 200000
maxDistanceKm = 15
```

Tổng phụ giỏ hàng:

```text
120000
```

Tính toán:

```text
straightDistanceKm = 4.4
estimatedRoadKm = 4.4 * 1.25 = 5.5
deliveryFee = 10000 + 5.5 * 5000 = 37500
total = 120000 + 37500 - discount
```

Nếu tổng phụ là `220000`:

```text
deliveryFee = 0
```

---

## 10. Ma Trận Xác Thực

| Trường Hợp | Kết Quả |
|---|---|
| Loại đơn là `PICKUP` | Phí vận chuyển = `0` |
| Loại đơn là `DELIVERY`, không có địa chỉ mặc định | Từ Chối |
| Địa chỉ mặc định thiếu tọa độ | Từ Chối |
| Chi nhánh thiếu tọa độ | Từ Chối |
| Khoảng cách vượt quá `max-distance-km` | Từ Chối |
| Tổng phụ vượt quá `free-threshold` | Phí vận chuyển = `0` |
| Đơn giao hàng hợp lệ | Tính phí theo khoảng cách |

---

## 11. Thương Lượng Thiết Kế

### Tại sao không gọi API bản đồ?

- API miễn phí có giới hạn lượt gọi và tỷ lệ.
- Quá trình thanh toán phải ổn định và nhanh chóng.
- Địa chỉ đã lưu trữ tọa độ.
- Haversine cộng hệ số đường đủ tốt cho giai đoạn đầu.

### Tại sao chỉ sử dụng địa chỉ mặc định?

- Request thanh toán đơn giản hơn.
- Ít nhánh xác thực hơn.
- Không tin tưởng các ID địa chỉ tùy ý từ request.
- Phù hợp với quyết định sản phẩm hiện tại: khách hàng nên đặt địa chỉ giao hàng mặc định trong hồ sơ.

### Tại sao tách dịch vụ?

- `OrderService` chỉ giữ vai trò điều phối.
- Quy tắc phí dễ kiểm tra hơn.
- Triển khai dựa trên API trong tương lai có thể thay thế chỉ `DeliveryFeeServiceImpl`.

---

## 12. Cải Tiến Trong Tương Lai

- Trả về `distanceKm` đã tính trong API xem trước đơn hàng.
- Thêm cột `delivery_distance_km` vào `od_order` nếu báo cáo cần.
- Th.setError codes:
  - `DELIVERY_001`: Yêu cầu địa chỉ mặc định
  - `DELIVERY_002`: Thiếu tọa độ địa chỉ
  - `DELIVERY_003`: Thiếu tọa độ chi nhánh
  - `DELIVERY_004`: Ngoài phạm vi phục vụ
- Thêm endpoint xem trước đơn hàng trước khi tạo:

```text
GET /api/v1/orders/preview
```

- Thêm unit test cho `DeliveryFeeServiceImpl` không cần cơ sở dữ liệu.
- Thay thế triển khai Haversine bằng chiến lược dựa trên nhà cung cấp nếu cần:

```text
DeliveryFeeService
-> HaversineDeliveryFeeService
-> MapApiDeliveryFeeService
```

---

## 13. Ghi Chú Mở

- Triển khai hiện tại từ chối các trường hợp khoảng cách/tọa độ không hợp lệ bằng các mã lỗi tổng quát hiện có.
- `default-fee` vẫn giữ trong cấu hình để tương thích ngược nhưng không phải là đầu vào chính của công thức.
- `deliveryAddressId` có thể được xóa khỏi `CreateOrderRequest` sau này nếu frontend không còn gửi nó.
