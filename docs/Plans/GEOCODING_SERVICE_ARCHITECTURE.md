# Kiến Trúc Geocoding Service

## Tổng Quan
Tài liệu này mô tả kiến trúc để triển khai Geocoding Service có khả năng mở rộng và linh hoạt cho ứng dụng Pine Drink. Service này sẽ tự động chuyển đổi địa chỉ thành tọa độ địa lý (latitude/longitude) để hỗ trợ tính toán khoảng cách giao hàng, hiển thị bản đồ và tối ưu hóa tuyến đường.

## Mục Lục
1. Yêu Cầu Nghiệp Vụ
2. Kiến Trúc Kỹ Thuật
3. So Sánh Các Provider
4. Các Giai Đoạn Triển Khai
5. Thiết Kế API
6. Database Schema
7. Cấu Hình
8. Xử Lý Lỗi
9. Cân Nhắc Về Performance
10. Tính Năng Mở Rộng Tương Lai

---

## 1. Yêu Cầu Nghiệp Vụ

### Yêu Cầu Chức Năng
- Tự động geocode địa chỉ khách hàng khi tạo/cập nhật
- Hỗ trợ nhiều geocoding provider với cơ chế fallback
- Cache kết quả geocoding để giảm API calls và chi phí
- Cho phép nhập tọa độ thủ công như phương án dự phòng
- Tính toán khoảng cách giao hàng từ cửa hàng đến khách hàng
- Xác thực địa chỉ có nằm trong vùng phục vụ không

### Yêu Cầu Phi Chức Năng
- Thời gian phản hồi: < 2 giây cho việc tạo địa chỉ
- Độ khả dụng: 99.9% uptime
- Chi phí: Tối thiểu hóa chi phí API thông qua caching
- Khả năng mở rộng: Xử lý 10,000+ địa chỉ
- Độ chính xác: Trong vòng 100 mét cho khu vực đô thị

---

## 2. Kiến Trúc Kỹ Thuật

### 2.1. Kiến Trúc Tổng Thể



### 2.2. Component Design

#### GeocodingService Interface


#### Strategy Pattern Implementation


---

## 3. So Sánh Các Provider

| Provider | Miễn Phí | Rate Limit | Độ Chính Xác | Hỗ Trợ VN | API Key | Ghi Chú |
|----------|----------|------------|--------------|-----------|---------|---------|
| **Nominatim** | ✅ Hoàn toàn | 1 req/s | ⭐⭐⭐ | ✅ Tốt | ❌ Không | Open source, dữ liệu OSM |
| **Google Maps** | 💰 00/tháng | 50 req/s | ⭐⭐⭐⭐⭐ | ✅ Rất tốt | ✅ Cần | Chính xác nhất |
| **Mapbox** | 💰 100k/tháng | 600 req/min | ⭐⭐⭐⭐ | ✅ Tốt | ✅ Cần | UI đẹp, dễ tích hợp |
| **LocationIQ** | 💰 5k/ngày | 2 req/s | ⭐⭐⭐ | ✅ Khá | ✅ Cần | Dựa trên OSM |

### Khuyến Nghị
- **Development/Testing**: Nominatim (miễn phí)
- **Production (Budget thấp)**: Nominatim + Cache + Fallback Google
- **Production (Chất lượng cao)**: Google Maps Primary + Nominatim Fallback

---

## 4. Các Giai Đoạn Triển Khai

### Phase 1: MVP (1-2 tuần)
**Mục tiêu**: Chức năng cơ bản với Nominatim

**Deliverables**:
- ✅ NominatimGeocodingService implementation
- ✅ Redis cache layer
- ✅ Tích hợp vào CustomerAddressService
- ✅ Basic error handling
- ✅ Unit tests

**Technical Tasks**:
1. Tạo GeocodingService interface
2. Implement NominatimGeocodingService
3. Tạo GeocodingCacheService với Redis
4. Update CustomerAddressService để gọi geocoding
5. Thêm configuration properties
6. Viết tests

### Phase 2: Production Ready (2-3 tuần)
**Mục tiêu**: Strategy pattern + Fallback + Monitoring

**Deliverables**:
- ✅ Strategy pattern cho multiple providers
- ✅ GoogleMapsGeocodingService implementation
- ✅ Fallback mechanism
- ✅ Rate limiting
- ✅ Retry logic với exponential backoff
- ✅ Metrics và monitoring
- ✅ Integration tests

**Technical Tasks**:
1. Refactor sang Strategy pattern
2. Implement GoogleMapsGeocodingService
3. Tạo GeocodingServiceFactory
4. Implement fallback chain
5. Thêm rate limiting với Resilience4j
6. Setup monitoring với Micrometer
7. Viết integration tests

### Phase 3: Advanced Features (3-4 tuần)
**Mục tiêu**: Async processing + Advanced caching + Analytics

**Deliverables**:
- ✅ Async geocoding với RabbitMQ
- ✅ Batch geocoding
- ✅ Distance calculation service
- ✅ Geofencing (kiểm tra vùng phục vụ)
- ✅ Analytics dashboard
- ✅ Self-hosted Nominatim (optional)

**Technical Tasks**:
1. Implement async geocoding với message queue
2. Tạo batch geocoding endpoint
3. Implement DistanceCalculationService
4. Tạo GeofencingService
5. Setup analytics với Elasticsearch
6. Deploy self-hosted Nominatim (nếu cần)

---

## 5. Thiết Kế API
