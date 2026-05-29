# Prompt Cho Frontend - Customer Address + Map Picker

Bạn là frontend engineer trong dự án Pine Drink. Hãy triển khai UI/logic quản lý địa chỉ khách hàng, hỗ trợ cả 2 flow nhập địa chỉ.

## Mục Tiêu

Triển khai màn hình/form quản lý địa chỉ khách hàng với 2 cách nhập:

1. Nhập địa chỉ thủ công
2. Chọn địa chỉ trên bản đồ

Backend đã hỗ trợ cả 2 flow.

---

## Flow 1: Nhập Địa Chỉ Thủ Công

User nhập các field:

- receiverName
- receiverPhone
- addressLine
- ward
- district
- city
- isDefault

Không cần chọn vị trí trên map.

Khi submit, gửi request tạo địa chỉ nhưng không gửi `latitude` và `longitude`.

Backend sẽ lưu địa chỉ trước, sau đó xử lý geocoding async để tự bổ sung tọa độ sau.

UI nên hiển thị ghi chú nhẹ:

```text
Tọa độ sẽ được tự động xác định sau.
```

Request mẫu:

```http
POST {{baseUrl}}/api/v1/customer/addresses
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
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

---

## Flow 2: Chọn Địa Chỉ Trên Bản Đồ

User nhập từ khóa tìm kiếm địa chỉ.

FE gọi API autocomplete:

```http
POST {{baseUrl}}/api/v1/geocoding/search
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "query": "Dai Co Viet",
  "limit": 5
}
```

Response mẫu:

```json
{
  "success": true,
  "message": "Found 5 addresses",
  "data": [
    {
      "displayName": "Đại Cồ Việt, Hai Bà Trưng, Hà Nội",
      "addressLine": "Đại Cồ Việt, Hai Bà Trưng, Hà Nội",
      "ward": "Bách Khoa",
      "district": "Hai Bà Trưng",
      "city": "Hà Nội",
      "country": "Việt Nam",
      "latitude": 21.005921,
      "longitude": 105.843112,
      "confidence": 0.95,
      "provider": "Nominatim"
    }
  ]
}
```

Khi user chọn một địa chỉ gợi ý:

- Center map tới `latitude` / `longitude`
- Đặt marker/pin tại vị trí đó
- Marker phải draggable
- Fill form bằng các field backend trả về nếu có:
  - displayName hoặc addressLine
  - ward
  - district
  - city
  - latitude
  - longitude

---

## Khi User Kéo Marker

Khi user kéo marker trên bản đồ:

1. Lấy `latitude` và `longitude` mới
2. Gọi reverse geocoding API
3. Update lại form nếu API có data
4. Luôn lưu lat/lng mới vào form

API:

```http
GET {{baseUrl}}/api/v1/geocoding/reverse?latitude={lat}&longitude={lng}
Authorization: Bearer {{accessToken}}
```

Ví dụ:

```http
GET {{baseUrl}}/api/v1/geocoding/reverse?latitude=21.036671&longitude=105.834662
```

Response mẫu:

```json
{
  "success": true,
  "message": "Address found successfully",
  "data": {
    "displayName": "Lăng Chủ tịch Hồ Chí Minh, 1, Đường Hùng Vương, Phường Ba Đình, Thành phố Hà Nội, Việt Nam",
    "addressLine": "Lăng Chủ tịch Hồ Chí Minh, 1, Đường Hùng Vương, Phường Ba Đình, Thành phố Hà Nội, Việt Nam",
    "ward": null,
    "district": "Phường Ba Đình",
    "city": "Thành phố Hà Nội",
    "country": "Việt Nam",
    "latitude": 21.0367831,
    "longitude": 105.8346888,
    "confidence": 0.43,
    "provider": "Nominatim"
  }
}
```

Nếu reverse geocoding không có data:

- Không crash UI
- Vẫn giữ lat/lng hiện tại
- Cho user tự chỉnh địa chỉ thủ công

---

## Submit Flow Map Picker

Khi user submit flow chọn trên bản đồ, gửi request tạo địa chỉ kèm `latitude` và `longitude`.

```http
POST {{baseUrl}}/api/v1/customer/addresses
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "receiverName": "Nguyen Van A",
  "receiverPhone": "0987654321",
  "addressLine": "So 1 Dai Co Viet",
  "ward": "Bach Khoa",
  "district": "Hai Ba Trung",
  "city": "Ha Noi",
  "latitude": 21.005921,
  "longitude": 105.843112,
  "isDefault": true
}
```

Backend sẽ lưu tọa độ trực tiếp, không cần async geocoding.

---

## API Customer Address Khác

### Lấy danh sách địa chỉ

```http
GET {{baseUrl}}/api/v1/customer/addresses
Authorization: Bearer {{accessToken}}
```

### Lấy chi tiết địa chỉ

```http
GET {{baseUrl}}/api/v1/customer/addresses/{addressId}
Authorization: Bearer {{accessToken}}
```

### Cập nhật địa chỉ

```http
PUT {{baseUrl}}/api/v1/customer/addresses/{addressId}
Authorization: Bearer {{accessToken}}
Content-Type: application/json
```

```json
{
  "receiverName": "Nguyen Van A Updated",
  "receiverPhone": "0987654321",
  "addressLine": "So 1 Dai Co Viet Updated",
  "ward": "Bach Khoa",
  "district": "Hai Ba Trung",
  "city": "Ha Noi",
  "latitude": 21.005921,
  "longitude": 105.843112,
  "isDefault": true
}
```

### Set default

```http
PATCH {{baseUrl}}/api/v1/customer/addresses/{addressId}/set-default
Authorization: Bearer {{accessToken}}
```

### Xóa địa chỉ

```http
DELETE {{baseUrl}}/api/v1/customer/addresses/{addressId}
Authorization: Bearer {{accessToken}}
```

---

## Yêu Cầu UX / Performance

- Search input phải debounce tối thiểu 500ms
- Không gọi search API khi query dưới 3 ký tự
- Không gọi API theo từng ký tự
- Hiển thị loading khi search hoặc reverse geocoding
- Nếu geocoding API lỗi, UI hiển thị thông báo nhẹ và không crash
- Cho phép user chỉnh tay addressLine, ward, district, city sau khi chọn map
- Validate receiverName, receiverPhone, addressLine bắt buộc
- Validate latitude/longitude trước khi submit flow map picker
- Nếu user chọn flow thủ công thì latitude/longitude có thể null
- Nếu user chọn flow map picker thì latitude/longitude là bắt buộc
- Nên cache tạm kết quả search trên FE theo query để giảm request lặp
- Tất cả request cần token nếu backend yêu cầu Authorization

---

## Gợi Ý UI

Form nên có tab hoặc radio:

```text
[ Nhập thủ công ] [ Chọn trên bản đồ ]
```

### Tab Nhập Thủ Công

- Tên người nhận
- Số điện thoại
- Địa chỉ
- Phường/Xã
- Quận/Huyện
- Tỉnh/Thành phố
- Checkbox địa chỉ mặc định
- Button lưu địa chỉ

### Tab Chọn Trên Bản Đồ

- Search box ở trên
- Dropdown suggestions bên dưới
- Map ở giữa
- Marker draggable
- Text hiển thị tọa độ hiện tại
- Các field địa chỉ vẫn cho chỉnh tay
- Button xác nhận/lưu địa chỉ

Mobile:

- Map cao khoảng 300-400px
- Search dropdown không che nút submit
- Marker dễ kéo

---

## Map Library Đề Xuất

Nếu chưa có provider trả phí:

- Leaflet
- OpenStreetMap tiles

Nếu dự án đã dùng Google Maps:

- Google Maps JavaScript SDK

Ưu tiên Leaflet nếu muốn miễn phí.

---

## Acceptance Criteria

- User có thể tạo địa chỉ bằng flow thủ công, không cần latitude/longitude
- User có thể search địa chỉ và xem danh sách gợi ý
- User chọn gợi ý thì marker nhảy đúng vị trí
- User có thể kéo marker và FE gọi reverse geocoding
- User submit map picker thì request có latitude/longitude
- Search API không bị spam nhờ debounce
- Nếu geocoding API lỗi, UI không crash
- User có thể chỉnh tay địa chỉ sau khi chọn từ map
- UI hoạt động tốt trên desktop và mobile
