# Hướng Dẫn Test APIs - Pine Drink Geocoding

## Chuẩn Bị

### 1. Cài Đặt Postman
- Download: https://www.postman.com/downloads/
- Hoặc dùng Postman Web: https://web.postman.com/

### 2. Khởi Động Backend
```bash
./mvnw spring-boot:run
```

Backend sẽ chạy tại: http://localhost:8080

---

## Bước 1: Test Geocoding APIs (Không Cần Login)

### 1.1. Search Address (Autocomplete)

**Request:**
```
POST http://localhost:8080/api/v1/geocoding/search
Content-Type: application/json

{
  "query": "Dai Co Viet",
  "limit": 5
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Found 5 addresses",
  "data": [
    {
      "displayName": "Đại Cồ Việt, Hai Bà Trưng, Hà Nội",
      "latitude": 21.005921,
      "longitude": 105.843112,
      "city": "Hà Nội",
      "district": "Hai Bà Trưng",
      "confidence": 0.95,
      "provider": "Nominatim"
    }
  ]
}
```

### 1.2. Reverse Geocoding

**Request:**
```
GET http://localhost:8080/api/v1/geocoding/reverse?latitude=21.005921&longitude=105.843112
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Address found successfully",
  "data": {
    "displayName": "Số 1, Đại Cồ Việt, Bách Khoa, Hai Bà Trưng, Hà Nội",
    "latitude": 21.005921,
    "longitude": 105.843112,
    "ward": "Bách Khoa",
    "district": "Hai Bà Trưng",
    "city": "Hà Nội"
  }
}
```

### 1.3. Check Service Status

**Request:**
```
GET http://localhost:8080/api/v1/geocoding/status
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Geocoding service is available",
  "data": true
}
```

---

## Bước 2: Login Để Lấy Access Token

### 2.1. Login

**Request:**
```
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "username": "your_username",
  "password": "your_password"
}
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 3600
  }
}
```

**⚠️ LƯU Ý:** Copy `accessToken` để dùng cho các request tiếp theo!

---

## Bước 3: Test Customer Address APIs (Cần Access Token)

### 3.1. Create Address - Flow 1 (Text Only, Async Geocoding)

**Request:**
```
POST http://localhost:8080/api/v1/customer/addresses
Content-Type: application/json
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE

{
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0987654321",
  "addressLine": "So 1 Dai Co Viet",
  "ward": "Bach Khoa",
  "district": "Hai Ba Trung",
  "city": "Ha Noi",
  "isDefault": true
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Customer address created successfully",
  "data": {
    "id": "uuid-here",
    "receiverName": "Nguyen Van A",
    "receiverPhone": "0987654321",
    "addressLine": "So 1 Dai Co Viet",
    "ward": "Bach Khoa",
    "district": "Hai Ba Trung",
    "city": "Ha Noi",
    "latitude": null,
    "longitude": null,
    "isDefault": true,
    "createdAt": "2026-05-28T10:20:00"
  }
}
```

**⚠️ Chú ý:** 
- `latitude` và `longitude` ban đầu là `null`
- Sau vài giây, background worker sẽ tự động cập nhật tọa độ
- Gọi lại API "Get Address By ID" để xem tọa độ đã được cập nhật chưa

### 3.2. Create Address - Flow 2 (With Coordinates from Map)

**Request:**
```
POST http://localhost:8080/api/v1/customer/addresses
Content-Type: application/json
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE

{
  "receiverName": "Tran Thi B",
  "receiverPhone": "0912345678",
  "addressLine": "So 144 Xuan Thuy",
  "ward": "Dich Vong Hau",
  "district": "Cau Giay",
  "city": "Ha Noi",
  "latitude": 21.028511,
  "longitude": 105.804817,
  "isDefault": false
}
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Customer address created successfully",
  "data": {
    "id": "uuid-here",
    "latitude": 21.028511,
    "longitude": 105.804817,
    ...
  }
}
```

**⚠️ Chú ý:** 
- `latitude` và `longitude` có giá trị ngay lập tức
- Không cần async geocoding

### 3.3. Get All My Addresses

**Request:**
```
GET http://localhost:8080/api/v1/customer/addresses
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
```

**Expected Response:**
```json
{
  "success": true,
  "message": "Customer addresses retrieved successfully",
  "data": [
    {
      "id": "uuid-1",
      "receiverName": "Nguyen Van A",
      "latitude": 21.005921,
      "longitude": 105.843112,
      "isDefault": true
    },
    {
      "id": "uuid-2",
      "receiverName": "Tran Thi B",
      "latitude": 21.028511,
      "longitude": 105.804817,
      "isDefault": false
    }
  ]
}
```

### 3.4. Get Address By ID

**Request:**
```
GET http://localhost:8080/api/v1/customer/addresses/{addressId}
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
```

### 3.5. Update Address

**Request:**
```
PUT http://localhost:8080/api/v1/customer/addresses/{addressId}
Content-Type: application/json
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE

{
  "receiverName": "Nguyen Van A Updated",
  "receiverPhone": "0987654321",
  "addressLine": "So 1 Dai Co Viet (Updated)",
  "ward": "Bach Khoa",
  "district": "Hai Ba Trung",
  "city": "Ha Noi",
  "latitude": 21.005921,
  "longitude": 105.843112,
  "isDefault": true
}
```

### 3.6. Set Address as Default

**Request:**
```
PATCH http://localhost:8080/api/v1/customer/addresses/{addressId}/set-default
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
```

### 3.7. Delete Address

**Request:**
```
DELETE http://localhost:8080/api/v1/customer/addresses/{addressId}
Authorization: Bearer YOUR_ACCESS_TOKEN_HERE
```

---

## Test Scenarios

### Scenario 1: User Nhập Địa Chỉ Thủ Công
1. Login → Lấy access token
2. Create address (Flow 1) - không có lat/lng
3. Đợi 5-10 giây
4. Get address by ID → Kiểm tra lat/lng đã được cập nhật

### Scenario 2: User Chọn Từ Map
1. Search address → Lấy gợi ý
2. User chọn 1 địa chỉ từ kết quả
3. Create address (Flow 2) - có lat/lng ngay
4. Get all addresses → Verify

### Scenario 3: User Kéo Pin Trên Map
1. User kéo pin đến vị trí mới
2. Reverse geocoding → Lấy địa chỉ từ tọa độ
3. Create address với địa chỉ + tọa độ mới

---

## Kiểm Tra Cache

### Test Cache Hit
1. Search "Dai Co Viet" lần 1 → Gọi API Nominatim
2. Search "Dai Co Viet" lần 2 → Lấy từ Redis cache (nhanh hơn)

**Cách kiểm tra:**
- Xem log backend
- Lần 1: "Nominatim search returned X results"
- Lần 2: "Cache hit for search query"

---

## Troubleshooting

### Lỗi 401 Unauthorized
- Kiểm tra access token có đúng không
- Token có hết hạn không (3600s = 1 giờ)
- Header Authorization có đúng format: `Bearer {token}`

### Lỗi 404 Not Found
- Kiểm tra URL có đúng không
- Backend có đang chạy không

### Geocoding Không Trả Về Kết Quả
- Kiểm tra internet connection
- Nominatim có thể bị rate limit (1 req/s)
- Thử địa chỉ khác

### Async Geocoding Không Cập Nhật
- Kiểm tra RabbitMQ có chạy không
- Xem log backend có lỗi không
- Kiểm tra Redis có hoạt động không

---

## Tips

1. **Dùng Postman Environment Variables:**
   - `baseUrl`: http://localhost:8080
   - `accessToken`: (auto-save sau login)
   - `addressId`: (auto-save sau create)

2. **Test Script trong Postman:**
```javascript
// Sau khi login, tự động lưu token
pm.collectionVariables.set("accessToken", pm.response.json().data.accessToken);

// Sau khi create address, tự động lưu ID
pm.collectionVariables.set("addressId", pm.response.json().data.id);
```

3. **Debounce ở FE:**
   - Đợi 500ms sau khi user ngừng gõ mới gọi search API
   - Tránh spam requests

---

Chúc bạn test thành công! 🚀
