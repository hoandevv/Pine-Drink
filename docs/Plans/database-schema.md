# Pine Drink Database Schema Documentation

## Tổng quan

Hệ thống gồm **9 file migration** với các schema chính:

| Prefix | Module | Số bảng |
|--------|--------|---------|
| `ce_` | Company / Brand / Branch | 6 |
| `ia_` | Identity & Access | 8 |
| `cu_` | Customer | 4 |
| `pr_` | Product & Menu | 5 |
| `mn_` | Many-to-Many (Branch) | 2 |
| `ca_` | Cart | 3 |
| `od_` | Order | 4 |
| `vc_` | Voucher | 3 |
| `py_` | Payment | 5 |
| `nt_` | Notification | 2 |
| `rp_` | Report | 1 |
| `pf_` | Platform | 2 |
| `iv_` | Inventory | 5 |

**Tổng cộng: 50 bảng**

---

## 1. ce_ — Company / Brand / Branch

### ce_brand
Thương hiệu — cấp cao nhất của hệ thống.

| Cột | Kiểu | Bắt buộc | Mô tả |
|-----|------|----------|-------|
| id | CHAR(36) | PK | Mã định danh |
| code | VARCHAR(50) | NOT NULL, UNIQUE | Mã thương hiệu |
| name | VARCHAR(150) | NOT NULL | Tên thương hiệu |
| legal_name | VARCHAR(200) | NULL | Tên pháp lý |
| tax_code | VARCHAR(50) | NULL | Mã số thuế |
| address | VARCHAR(255) | NULL | Địa chỉ |
| phone | VARCHAR(20) | NULL | Số điện thoại |
| email | VARCHAR(150) | NULL | Email |
| timezone | VARCHAR(50) | DEFAULT 'Asia/Ho_Chi_Minh' | Múi giờ |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | Trạng thái |
| created_at | DATETIME | NOT NULL | Thời gian tạo |
| created_by | CHAR(36) | NULL | Người tạo |
| updated_at | DATETIME | NOT NULL | Thời gian cập nhật |
| updated_by | CHAR(36) | NULL | Người cập nhật |

### ce_brand_domain
Domain và public key cho từng thương hiệu (dùng cho multi-tenant).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) | Thương hiệu |
| domain | VARCHAR(255) | NOT NULL | Tên miền |
| public_key | VARCHAR(100) | NOT NULL, UNIQUE | Public key |
| channel | VARCHAR(30) | DEFAULT 'WEB' | Kênh (WEB/APP...) |
| allow_public_register | BOOLEAN | DEFAULT TRUE | Cho phép đăng ký công khai |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| created_at/updated_at... | | | |

### ce_branch
Chi nhánh cửa hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) | Thương hiệu |
| code | VARCHAR(50) | NOT NULL, UNIQUE(brand_id) | Mã chi nhánh |
| name | VARCHAR(150) | NOT NULL | Tên chi nhánh |
| address | VARCHAR(255) | NULL | Địa chỉ |
| phone | VARCHAR(20) | NULL | Số điện thoại |
| email | VARCHAR(150) | NULL | Email |
| latitude | DECIMAL(10,7) | NULL | Vĩ độ |
| longitude | DECIMAL(10,7) | NULL | Kinh độ |
| timezone | VARCHAR(50) | DEFAULT | Múi giờ |
| supports_pickup | BOOLEAN | DEFAULT TRUE | Hỗ trợ mang đi |
| supports_delivery | BOOLEAN | DEFAULT FALSE | Hỗ trợ giao hàng |
| average_preparation_minutes | INT | DEFAULT 15 | Thời gian chuẩn bị TB |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| created_at/updated_at... | | | |

### ce_branch_hours
Giờ mở cửa theo ngày trong tuần của chi nhánh.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| branch_id | CHAR(36) | FK → ce_branch(id) | Chi nhánh |
| day_of_week | TINYINT | CHECK 1-7 | Thứ (2=1, CN=7) |
| open_time | TIME | NOT NULL | Giờ mở |
| close_time | TIME | NOT NULL | Giờ đóng |
| is_closed | BOOLEAN | DEFAULT FALSE | Có đóng cửa? |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (branch_id, day_of_week) | |

### ce_pickup_time_slot
Khung giờ cho phép khách lấy hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| branch_id | CHAR(36) | FK → ce_branch(id) | Chi nhánh |
| slot_code | VARCHAR(50) | NOT NULL | Mã khung giờ |
| start_time | TIME | NOT NULL | Giờ bắt đầu |
| end_time | TIME | NOT NULL | Giờ kết thúc |
| max_orders | INT | NULL | Số đơn tối đa |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (branch_id, slot_code) | |

### ce_setting
Cấu hình linh hoạt cho brand/branch.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) | NULL = toàn hệ thống |
| branch_id | CHAR(36) | FK → ce_branch(id) | NULL = toàn brand |
| config_key | VARCHAR(100) | NOT NULL | Tên cấu hình |
| config_value | VARCHAR(500) | NOT NULL | Giá trị |
| data_type | VARCHAR(20) | NOT NULL | Kiểu dữ liệu |
| description | VARCHAR(255) | NULL | Mô tả |
| is_runtime_editable | BOOLEAN | DEFAULT TRUE | Có thể sửa runtime? |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, branch_id, config_key) | |

---

## 2. ia_ — Identity & Access

### ia_account
Tài khoản nhân viên/quản trị.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) SET NULL | Thương hiệu |
| username | VARCHAR(100) | NOT NULL, UNIQUE | Tên đăng nhập |
| password | VARCHAR(255) | NOT NULL | Mật khẩu (hash) |
| full_name | VARCHAR(150) | NOT NULL | Họ tên |
| email | VARCHAR(150) | UNIQUE | Email |
| phone | VARCHAR(20) | UNIQUE | Số điện thoại |
| avatar_url | VARCHAR(500) | NULL | Ảnh đại diện |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| last_login_at | DATETIME | NULL | Lần đăng nhập cuối |
| created_at/updated_at... | | | |

### ia_role
Vai trò (Role) — phân quyền.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| code | VARCHAR(80) | NOT NULL, UNIQUE | Mã vai trò (vd: ADMIN) |
| name | VARCHAR(150) | NOT NULL | Tên vai trò |
| description | VARCHAR(255) | NULL | Mô tả |
| role_type | VARCHAR(30) | DEFAULT 'SYSTEM' | Loại (SYSTEM/CUSTOM) |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| created_at/updated_at... | | | |

### ia_permission
Quyền hạn chi tiết.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| code | VARCHAR(120) | NOT NULL, UNIQUE | Mã quyền |
| name | VARCHAR(150) | NOT NULL | Tên quyền |
| module | VARCHAR(80) | NOT NULL | Module |
| description | VARCHAR(255) | NULL | Mô tả |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| created_at/updated_at... | | | |

### ia_role_permission
Gán quyền cho vai trò (N-N).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| role_id | CHAR(36) | FK → ia_role(id) CASCADE | Vai trò |
| permission_id | CHAR(36) | FK → ia_permission(id) CASCADE | Quyền |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (role_id, permission_id) | |

### ia_scope
Phạm vi áp dụng phân quyền (toàn brand, một branch, toàn hệ thống).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| scope_type | VARCHAR(30) | NOT NULL | Loại: SYSTEM/BRAND/BRANCH |
| brand_id | CHAR(36) | FK → ce_brand(id) | |
| branch_id | CHAR(36) | FK → ce_branch(id) | |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (scope_type, brand_id, branch_id) | |

### ia_account_role_assignment
Gán vai trò cho tài khoản trong một phạm vi.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| account_id | CHAR(36) | FK → ia_account(id) CASCADE | Tài khoản |
| role_id | CHAR(36) | FK → ia_role(id) RESTRICT | Vai trò |
| scope_id | CHAR(36) | FK → ia_scope(id) RESTRICT | Phạm vi |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| assigned_at | DATETIME | DEFAULT NOW | Thời gian gán |
| assigned_by | CHAR(36) | NULL | Người gán |
| expires_at | DATETIME | NULL | Hết hạn |
| UNIQUE | | (account_id, role_id, scope_id) | |

### ia_audit_log
Nhật ký kiểm tra.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| actor_account_id | CHAR(36) | FK → ia_account(id) |
| action | VARCHAR(100) | Hành động (CREATE/UPDATE/DELETE...) |
| module | VARCHAR(80) | Module |
| target_type | VARCHAR(80) | Loại đối tượng |
| target_id | CHAR(36) | ID đối tượng |
| brand_id | CHAR(36) | |
| branch_id | CHAR(36) | |
| ip_address | VARCHAR(64) | |
| user_agent | VARCHAR(500) | |
| before_data | JSON | Dữ liệu trước |
| after_data | JSON | Dữ liệu sau |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' |
| created_at | DATETIME | |

### ia_refresh_token
Token làm mới cho JWT.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| account_id | CHAR(36) | FK → ia_account(id) CASCADE | Tài khoản |
| token_hash | VARCHAR(255) | UNIQUE | Token hash |
| device_info | VARCHAR(255) | NULL | Thông tin thiết bị |
| ip_address | VARCHAR(64) | NULL | IP |
| expires_at | DATETIME | NOT NULL | Hết hạn |
| revoked_at | DATETIME | NULL | Thời gian thu hồi |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| created_at/updated_at... | | | |

---

## 3. cu_ — Customer

### cu_customer_profile
Hồ sơ khách hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| account_id | CHAR(36) | FK → ia_account(id) CASCADE, UNIQUE | Tài khoản liên kết |
| customer_code | VARCHAR(50) | UNIQUE | Mã khách hàng |
| full_name | VARCHAR(150) | NOT NULL | Họ tên |
| phone | VARCHAR(20) | NULL, INDEX | Số điện thoại |
| email | VARCHAR(150) | NULL, INDEX | Email |
| date_of_birth | DATE | NULL | Ngày sinh |
| gender | VARCHAR(20) | NULL | Giới tính |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### cu_customer_address
Địa chỉ giao hàng của khách hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| customer_id | CHAR(36) | FK → cu_customer_profile(id) CASCADE | Khách hàng |
| receiver_name | VARCHAR(150) | NOT NULL | Tên người nhận |
| receiver_phone | VARCHAR(20) | NOT NULL | SĐT người nhận |
| address_line | VARCHAR(255) | NOT NULL | Địa chỉ |
| ward | VARCHAR(100) | NULL | Phường/xã |
| district | VARCHAR(100) | NULL | Quận/huyện |
| city | VARCHAR(100) | NULL | Tỉnh/thành |
| latitude | DECIMAL(10,7) | NULL | Vĩ độ |
| longitude | DECIMAL(10,7) | NULL | Kinh độ |
| is_default | BOOLEAN | DEFAULT FALSE | Mặc định |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### cu_loyalty_account
Tài khoản tích điểm thành viên.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| customer_id | CHAR(36) | FK → cu_customer_profile(id) CASCADE, UNIQUE | Khách hàng |
| tier | VARCHAR(30) | DEFAULT 'SILVER' | Hạng: SILVER/GOLD/PLATINUM |
| points_balance | INT | DEFAULT 0, CHECK >=0 | Điểm hiện tại |
| lifetime_points | INT | DEFAULT 0, CHECK >=0 | Điểm tích lũy |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### cu_loyalty_point_history
Lịch sử biến động điểm.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| loyalty_account_id | CHAR(36) | FK → cu_loyalty_account(id) CASCADE | Tài khoản loyalty |
| order_id | CHAR(36) | NULL | Đơn hàng |
| transaction_type | VARCHAR(30) | NOT NULL | EARN/REDEEM/EXPIRE... |
| points | INT | NOT NULL | Số điểm (+/-) |
| balance_after | INT | NOT NULL | Số dư sau |
| reason | VARCHAR(255) | NULL | Lý do |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

---

## 4. pr_ — Product & Menu

### pr_category
Danh mục sản phẩm.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | Thương hiệu |
| code | VARCHAR(50) | NOT NULL | Mã danh mục |
| name | VARCHAR(150) | NOT NULL | Tên danh mục |
| description | VARCHAR(255) | NULL | Mô tả |
| image_url | VARCHAR(500) | NULL | Ảnh danh mục |
| display_order | INT | DEFAULT 0 | Thứ tự hiển thị |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, code) | |

### pr_product
Sản phẩm / đồ uống.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | Thương hiệu |
| category_id | CHAR(36) | FK → pr_category(id) RESTRICT | Danh mục |
| code | VARCHAR(50) | NOT NULL | Mã sản phẩm |
| name | VARCHAR(150) | NOT NULL | Tên sản phẩm |
| description | VARCHAR(500) | NULL | Mô tả |
| image_url | VARCHAR(500) | NULL | Ảnh sản phẩm |
| base_price | DECIMAL(12,2) | NOT NULL, CHECK >=0 | Giá cơ bản |
| preparation_minutes | INT | DEFAULT 10 | Phút chuẩn bị |
| is_featured | BOOLEAN | DEFAULT FALSE | Nổi bật |
| is_best_seller | BOOLEAN | DEFAULT FALSE | Bán chạy |
| available_ice_levels | VARCHAR(50) | DEFAULT '0,30,50,70,100' | Các mức đá (phần trăm) |
| available_sugar_levels | VARCHAR(50) | DEFAULT '0,30,50,70,100' | Các mức đường (phần trăm) |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, code) | |

### pr_product_variant
Biến thể sản phẩm (Size Nhỏ/Vừa/Lớn).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| product_id | CHAR(36) | FK → pr_product(id) CASCADE | Sản phẩm |
| variant_code | VARCHAR(50) | NOT NULL | Mã biến thể |
| variant_name | VARCHAR(100) | NOT NULL | Tên (vd: Nhỏ, Vừa, Lớn) |
| size_label | VARCHAR(30) | NOT NULL | Nhãn size |
| price_delta | DECIMAL(12,2) | DEFAULT 0 | Chênh lệch giá |
| display_order | INT | DEFAULT 0 | Thứ tự |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (product_id, variant_code) | |

### pr_topping
Topping (trân châu, thạch, kem, pudding...).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | Thương hiệu |
| code | VARCHAR(50) | NOT NULL | Mã topping |
| name | VARCHAR(150) | NOT NULL | Tên topping |
| price | DECIMAL(12,2) | NOT NULL, CHECK >=0 | Giá |
| image_url | VARCHAR(500) | NULL | Ảnh topping |
| group_name | VARCHAR(100) | NULL | Nhóm (Trân châu/Thạch/Kem/Pudding) |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, code) | |

### pr_product_topping
Sản phẩm có thể thêm topping nào (N-N).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| product_id | CHAR(36) | FK → pr_product(id) CASCADE | Sản phẩm |
| topping_id | CHAR(36) | FK → pr_topping(id) RESTRICT | Topping |
| is_default | BOOLEAN | DEFAULT FALSE | Mặc định? |
| max_quantity | INT | DEFAULT 3, CHECK >=1 | Số lượng tối đa |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (product_id, topping_id) | |

---

## 5. mn_ — Many-to-Many (Branch availability)

### mn_branch_product_availability
Sản phẩm còn bán ở chi nhánh nào.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| branch_id | CHAR(36) | FK → ce_branch(id) CASCADE | Chi nhánh |
| product_id | CHAR(36) | FK → pr_product(id) CASCADE | Sản phẩm |
| is_available | BOOLEAN | DEFAULT TRUE | Còn bán? |
| sale_price | DECIMAL(12,2) | NULL, CHECK >=0 | Giá bán riêng (nếu khác) |
| sold_out_reason | VARCHAR(255) | NULL | Lý do hết hàng |
| available_from | DATETIME | NULL | Bắt đầu bán |
| available_to | DATETIME | NULL | Kết thúc bán |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (branch_id, product_id) | |

### mn_branch_topping_availability
Topping còn bán ở chi nhánh nào.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| branch_id | CHAR(36) | FK → ce_branch(id) CASCADE | Chi nhánh |
| topping_id | CHAR(36) | FK → pr_topping(id) CASCADE | Topping |
| is_available | BOOLEAN | DEFAULT TRUE | Còn bán? |
| sold_out_reason | VARCHAR(255) | NULL | Lý do hết hàng |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (branch_id, topping_id) | |

---

## 6. ca_ — Cart

### ca_cart
Giỏ hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| customer_id | CHAR(36) | FK → cu_customer_profile(id) CASCADE | Khách hàng (NULL nếu guest) |
| branch_id | CHAR(36) | FK → ce_branch(id) RESTRICT | Chi nhánh |
| session_id | VARCHAR(120) | NULL | Session ID (cho guest) |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | ACTIVE → COMPLETED/ABANDONED |
| created_at/updated_at... | | | |

### ca_cart_item
Sản phẩm trong giỏ hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| cart_id | CHAR(36) | FK → ca_cart(id) CASCADE | Giỏ hàng |
| product_id | CHAR(36) | FK → pr_product(id) RESTRICT | Sản phẩm |
| variant_id | CHAR(36) | FK → pr_product_variant(id) SET NULL | Biến thể (size) |
| quantity | INT | CHECK >0 | Số lượng |
| sugar_level | VARCHAR(30) | DEFAULT 'NORMAL' | Lượng đường |
| ice_level | VARCHAR(30) | DEFAULT 'NORMAL' | Lượng đá |
| note | VARCHAR(255) | NULL | Ghi chú |
| unit_price | DECIMAL(12,2) | CHECK >=0 | Đơn giá |
| total_price | DECIMAL(12,2) | CHECK >=0 | Thành tiền |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### ca_cart_item_topping
Topping của sản phẩm trong giỏ.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| cart_item_id | CHAR(36) | FK → ca_cart_item(id) CASCADE | Item trong giỏ |
| topping_id | CHAR(36) | FK → pr_topping(id) RESTRICT | Topping |
| quantity | INT | DEFAULT 1, CHECK >0 | Số lượng |
| unit_price | DECIMAL(12,2) | CHECK >=0 | Đơn giá |
| total_price | DECIMAL(12,2) | CHECK >=0 | Thành tiền |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

---

## 7. od_ — Order

### od_order
Đơn hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| order_code | VARCHAR(50) | UNIQUE | Mã đơn hàng |
| brand_id | CHAR(36) | FK → ce_brand(id) RESTRICT | Thương hiệu |
| branch_id | CHAR(36) | FK → ce_branch(id) RESTRICT | Chi nhánh |
| customer_id | CHAR(36) | FK → cu_customer_profile(id) SET NULL | Khách hàng (NULL = guest) |
| customer_name | VARCHAR(150) | NOT NULL | Tên khách |
| customer_phone | VARCHAR(20) | NOT NULL | SĐT khách |
| customer_email | VARCHAR(150) | NULL | Email khách |
| order_type | VARCHAR(30) | DEFAULT 'PICKUP' | PICKUP/DELIVERY |
| status | VARCHAR(30) | DEFAULT 'PENDING' | PENDING/CONFIRMED/PREPARING/READY/COMPLETED/CANCELLED |
| payment_status | VARCHAR(30) | DEFAULT 'UNPAID' | UNPAID/PAID/REFUNDED |
| subtotal_amount | DECIMAL(12,2) | CHECK >=0 | Tạm tính |
| discount_amount | DECIMAL(12,2) | CHECK >=0 | Giảm giá |
| delivery_fee | DECIMAL(12,2) | CHECK >=0 | Phí giao hàng |
| total_amount | DECIMAL(12,2) | CHECK >=0 | Tổng tiền |
| pickup_time | DATETIME | NULL | Giờ lấy hàng |
| delivery_address | VARCHAR(255) | NULL | Địa chỉ giao |
| note | VARCHAR(500) | NULL | Ghi chú |
| confirmed_at | DATETIME | NULL | |
| prepared_at | DATETIME | NULL | |
| ready_at | DATETIME | NULL | |
| completed_at | DATETIME | NULL | |
| cancelled_at | DATETIME | NULL | |
| cancel_reason | VARCHAR(255) | NULL | |
| created_at/updated_at... | | | |

### od_order_item
Sản phẩm trong đơn hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| order_id | CHAR(36) | FK → od_order(id) CASCADE | Đơn hàng |
| product_id | CHAR(36) | FK → pr_product(id) SET NULL | |
| variant_id | CHAR(36) | FK → pr_product_variant(id) SET NULL | |
| product_code | VARCHAR(50) | NOT NULL | Mã SP (snapshot) |
| product_name | VARCHAR(150) | NOT NULL | Tên SP (snapshot) |
| variant_name | VARCHAR(100) | NULL | Tên size (snapshot) |
| quantity | INT | CHECK >0 | |
| sugar_level | VARCHAR(30) | DEFAULT 'NORMAL' | |
| ice_level | VARCHAR(30) | DEFAULT 'NORMAL' | |
| note | VARCHAR(255) | NULL | |
| unit_price | DECIMAL(12,2) | CHECK >=0 | |
| total_price | DECIMAL(12,2) | CHECK >=0 | |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### od_order_item_topping
Topping trong sản phẩm của đơn hàng.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| order_item_id | CHAR(36) | FK → od_order_item(id) CASCADE | |
| topping_id | CHAR(36) | FK → pr_topping(id) SET NULL | |
| topping_code | VARCHAR(50) | NOT NULL | Mã topping (snapshot) |
| topping_name | VARCHAR(150) | NOT NULL | Tên topping (snapshot) |
| quantity | INT | DEFAULT 1, CHECK >0 | |
| unit_price | DECIMAL(12,2) | CHECK >=0 | |
| total_price | DECIMAL(12,2) | CHECK >=0 | |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### od_order_status_history
Lịch sử thay đổi trạng thái đơn hàng.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| order_id | CHAR(36) | FK → od_order(id) CASCADE |
| old_status | VARCHAR(30) | NULL |
| new_status | VARCHAR(30) | NOT NULL |
| reason | VARCHAR(255) | NULL |
| changed_by | CHAR(36) | FK → ia_account(id) SET NULL |
| changed_at | DATETIME | DEFAULT NOW |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' |

---

## 8. vc_ — Voucher

### vc_voucher
Phiếu giảm giá / Voucher.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | Thương hiệu |
| code | VARCHAR(80) | UNIQUE(brand_id) | Mã voucher |
| name | VARCHAR(150) | NOT NULL | Tên |
| description | VARCHAR(500) | NULL | Mô tả |
| discount_type | VARCHAR(30) | NOT NULL | PERCENTAGE/FIXED |
| discount_value | DECIMAL(12,2) | CHECK >=0 | Giá trị giảm |
| max_discount_amount | DECIMAL(12,2) | NULL, CHECK >=0 | Giảm tối đa |
| min_order_amount | DECIMAL(12,2) | DEFAULT 0 | Đơn tối thiểu |
| usage_limit | INT | NULL | Giới hạn lượt dùng |
| used_count | INT | DEFAULT 0 | Đã dùng |
| usage_limit_per_customer | INT | NULL | Giới hạn/khách |
| start_at | DATETIME | NOT NULL | Bắt đầu |
| end_at | DATETIME | NOT NULL | Kết thúc |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### vc_voucher_branch
Voucher áp dụng cho chi nhánh nào (N-N).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| voucher_id | CHAR(36) | FK → vc_voucher(id) CASCADE | |
| branch_id | CHAR(36) | FK → ce_branch(id) CASCADE | |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (voucher_id, branch_id) | |

### vc_voucher_usage
Lịch sử sử dụng voucher.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| voucher_id | CHAR(36) | FK → vc_voucher(id) RESTRICT | |
| order_id | CHAR(36) | FK → od_order(id) CASCADE | |
| customer_id | CHAR(36) | FK → cu_customer_profile(id) SET NULL | |
| discount_amount | DECIMAL(12,2) | CHECK >=0 | Số tiền giảm |
| used_at | DATETIME | DEFAULT NOW | |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (voucher_id, order_id) | |

---

## 9. py_ — Payment

### py_payment_intent
Phiên thanh toán (theo provider).

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| order_id | CHAR(36) | FK → od_order(id) CASCADE | Đơn hàng |
| provider | VARCHAR(50) | NOT NULL | Ví dụ: VNPAY, STRIPE |
| amount | DECIMAL(12,2) | CHECK >=0 | |
| currency | VARCHAR(10) | DEFAULT 'VND' | |
| status | VARCHAR(30) | DEFAULT 'PENDING' | |
| request_payload | JSON | NULL | |
| response_payload | JSON | NULL | |
| expires_at | DATETIME | NULL | |
| UNIQUE | | (order_id, provider) | |

### py_transaction
Giao dịch thanh toán.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| order_id | CHAR(36) | FK → od_order(id) RESTRICT | |
| payment_intent_id | CHAR(36) | FK → py_payment_intent(id) SET NULL | |
| transaction_code | VARCHAR(100) | UNIQUE | Mã giao dịch |
| provider | VARCHAR(50) | NOT NULL | |
| payment_method | VARCHAR(50) | NOT NULL | |
| amount | DECIMAL(12,2) | CHECK >=0 | |
| currency | VARCHAR(10) | DEFAULT 'VND' | |
| status | VARCHAR(30) | DEFAULT 'PENDING' | |
| paid_at | DATETIME | NULL | |
| failed_reason | VARCHAR(255) | NULL | |

### py_callback_log
Log callback từ cổng thanh toán.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| provider | VARCHAR(50) | |
| transaction_code | VARCHAR(100) | NULL |
| request_id | VARCHAR(120) | NULL, UNIQUE(provider) |
| raw_payload | JSON | |
| signature_valid | BOOLEAN | DEFAULT FALSE |
| processing_status | VARCHAR(30) | DEFAULT 'PENDING' |
| error_message | VARCHAR(500) | NULL |
| received_at | DATETIME | |
| processed_at | DATETIME | NULL |

### py_refund
Yêu cầu hoàn tiền.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| transaction_id | CHAR(36) | FK → py_transaction(id) RESTRICT | |
| refund_code | VARCHAR(100) | UNIQUE | Mã hoàn tiền |
| amount | DECIMAL(12,2) | CHECK >=0 | |
| reason | VARCHAR(255) | NULL | |
| status | VARCHAR(30) | DEFAULT 'PENDING' | |
| requested_by | CHAR(36) | FK → ia_account(id) SET NULL | |
| requested_at | DATETIME | | |
| completed_at | DATETIME | NULL | |

---

## 10. nt_ — Notification

### nt_notification
Thông báo.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| recipient_account_id | CHAR(36) | FK → ia_account(id) CASCADE |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE |
| branch_id | CHAR(36) | FK → ce_branch(id) CASCADE |
| notification_type | VARCHAR(80) | Loại thông báo |
| title | VARCHAR(180) | Tiêu đề |
| content | VARCHAR(1000) | Nội dung |
| channel | VARCHAR(30) | DEFAULT 'IN_APP' |
| status | VARCHAR(30) | DEFAULT 'UNREAD' |
| reference_type | VARCHAR(80) | NULL |
| reference_id | CHAR(36) | NULL |
| metadata | JSON | NULL |
| sent_at | DATETIME | NULL |
| read_at | DATETIME | NULL |

### nt_template
Mẫu thông báo/email.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | NULL = mặc định |
| template_code | VARCHAR(100) | NOT NULL | Mã template |
| channel | VARCHAR(30) | NOT NULL | EMAIL/SMS/IN_APP |
| subject | VARCHAR(180) | NULL | Tiêu đề |
| body | TEXT | NOT NULL | Nội dung |
| variables | JSON | NULL | Biến động |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, template_code, channel) | |

---

## 11. rp_ — Report

### rp_export_request
Yêu cầu xuất báo cáo.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE |
| branch_id | CHAR(36) | FK → ce_branch(id) SET NULL |
| requested_by | CHAR(36) | FK → ia_account(id) RESTRICT |
| report_type | VARCHAR(80) | |
| file_format | VARCHAR(20) | DEFAULT 'XLSX' |
| filters | JSON | |
| status | VARCHAR(30) | DEFAULT 'PENDING' |
| file_url | VARCHAR(500) | |
| error_message | VARCHAR(500) | |
| requested_at | | |
| started_at | | |
| completed_at | | |

---

## 12. pf_ — Platform

### pf_outbox_event
Outbox pattern — sự kiện chờ gửi RabbitMQ.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| event_type | VARCHAR(120) | |
| aggregate_type | VARCHAR(80) | |
| aggregate_id | CHAR(36) | |
| brand_id | CHAR(36) | NULL |
| branch_id | CHAR(36) | NULL |
| routing_key | VARCHAR(150) | |
| payload | JSON | |
| status | VARCHAR(30) | DEFAULT 'PENDING' |
| retry_count | INT | DEFAULT 0 |
| max_retries | INT | DEFAULT 5 |
| next_retry_at | DATETIME | NULL |
| locked_by | VARCHAR(100) | NULL |
| locked_at | DATETIME | NULL |
| published_at | DATETIME | NULL |
| last_error | VARCHAR(1000) | NULL |

### pf_idempotency_key
Khóa idempotent — chống xử lý trùng request.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| idempotency_key | VARCHAR(150) | UNIQUE |
| request_hash | VARCHAR(255) | |
| response_body | JSON | |
| http_status | INT | |
| status | VARCHAR(30) | DEFAULT 'PROCESSING' |
| expires_at | DATETIME | |

---

## 13. iv_ — Inventory

### iv_ingredient
Nguyên liệu.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| brand_id | CHAR(36) | FK → ce_brand(id) CASCADE | |
| code | VARCHAR(50) | NOT NULL | Mã nguyên liệu |
| name | VARCHAR(150) | NOT NULL | Tên |
| unit | VARCHAR(30) | NOT NULL | Đơn vị (kg, ml, cái...) |
| min_stock_quantity | DECIMAL(12,3) | DEFAULT 0, CHECK >=0 | Tồn tối thiểu |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (brand_id, code) | |

### iv_recipe
Công thức pha chế.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| product_id | CHAR(36) | FK → pr_product(id) CASCADE | Sản phẩm |
| variant_id | CHAR(36) | FK → pr_product_variant(id) CASCADE | NULL = áp dụng mọi size |
| name | VARCHAR(150) | NOT NULL | Tên công thức |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |

### iv_recipe_item
Nguyên liệu trong công thức.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| recipe_id | CHAR(36) | FK → iv_recipe(id) CASCADE | |
| ingredient_id | CHAR(36) | FK → iv_ingredient(id) RESTRICT | |
| quantity | DECIMAL(12,3) | CHECK >0 | Số lượng |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (recipe_id, ingredient_id) | |

### iv_stock
Tồn kho nguyên liệu theo chi nhánh.

| Cột | Kiểu | Ràng buộc | Mô tả |
|-----|------|-----------|-------|
| id | CHAR(36) | PK | |
| branch_id | CHAR(36) | FK → ce_branch(id) CASCADE | |
| ingredient_id | CHAR(36) | FK → iv_ingredient(id) RESTRICT | |
| quantity_on_hand | DECIMAL(12,3) | DEFAULT 0, CHECK >=0 | Tồn thực tế |
| reserved_quantity | DECIMAL(12,3) | DEFAULT 0, CHECK >=0 | Đã đặt trước |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' | |
| UNIQUE | | (branch_id, ingredient_id) | |

### iv_stock_movement
Lịch sử nhập/xuất kho.

| Cột | Kiểu | Mô tả |
|-----|------|-------|
| id | CHAR(36) | PK |
| branch_id | CHAR(36) | FK → ce_branch(id) RESTRICT |
| ingredient_id | CHAR(36) | FK → iv_ingredient(id) RESTRICT |
| order_id | CHAR(36) | FK → od_order(id) SET NULL |
| movement_type | VARCHAR(40) | IMPORT/EXPORT/ADJUST/RETURN |
| quantity | DECIMAL(12,3) | |
| before_quantity | DECIMAL(12,3) | |
| after_quantity | DECIMAL(12,3) | |
| reason | VARCHAR(255) | NULL |
| status | VARCHAR(30) | DEFAULT 'ACTIVE' |

---

## Tổng quan quan hệ giữa các schema

```
ce_brand ─── ce_brand_domain
     │
     ├── ce_branch ─── ce_branch_hours
     │         │      ─── ce_pickup_time_slot
     │         │      ─── ce_setting
     │         │
     │         ├── mn_branch_product_availability
     │         ├── mn_branch_topping_availability
     │         ├── ca_cart
     │         ├── od_order
     │         ├── vc_voucher_branch
     │         ├── iv_stock
     │         └── iv_stock_movement
     │
     ├── pr_category ─── pr_product ─── pr_product_variant
     │         │              │
     │         │              ├── pr_product_topping ─── pr_topping (group_name)
     │         │              │
     │         │              ├── ca_cart_item ─── ca_cart_item_topping
     │         │              ├── od_order_item ─── od_order_item_topping
     │         │              └── iv_recipe ─── iv_recipe_item ─── iv_ingredient
     │         │
     │         └── pr_topping ─── pr_product_topping
     │
     ├── ia_account ─── ia_account_role_assignment ─── ia_scope
     │         │              │
     │         │              └── ia_role ─── ia_role_permission ─── ia_permission
     │         │
     │         ├── ia_refresh_token
     │         └── ia_audit_log
     │
     ├── cu_customer_profile ─── cu_customer_address
     │         │
     │         └── cu_loyalty_account ─── cu_loyalty_point_history
     │
     ├── vc_voucher ─── vc_voucher_branch
     │         └── vc_voucher_usage
     │
     ├── py_payment_intent ─── py_transaction ─── py_refund
     │              └── py_callback_log
     │
     ├── nt_notification
     ├── nt_template
     └── rp_export_request
```

## Ghi chú chung

- Tất cả bảng dùng `CHAR(36)` làm UUID primary key, sinh tự động qua `@PrePersist` ở `BaseEntity.java`
- Các bảng đều kế thừa `BaseEntity` với các trường: `id`, `status`, `created_at`, `created_by`, `updated_at`, `updated_by`
- Chỉ có bảng `od_order` và `od_order_item` mới lưu snapshot thông tin (code, name) vì đơn hàng không được thay đổi sau khi tạo
- Schema prefix `ce_` = Company Entity, `ia_` = Identity Access, `cu_` = Customer, `pr_` = Product, `mn_` = Many-to-Many, `ca_` = Cart, `od_` = Order, `vc_` = Voucher, `py_` = Payment, `nt_` = Notification, `rp_` = Report, `pf_` = Platform, `iv_` = Inventory
