# Technical Design Document — Core Domain CRUD

| **Field** | **Value** |
|-----------|-----------|
| **Document ID** | TDD-PINE-CORE-002 |
| **Project** | Pine Drink — Hệ thống order đồ uống online |
| **Module** | Core Domain: Brand, Branch, Category, Product, ProductVariant, Topping |
| **Version** | 1.0 |
| **Status** | Draft |
| **Author** | Pine Drink Dev Team |
| **Last Updated** | 2026-05-29 |

---

## 1. Title

**Xây dựng CRUD đầy đủ cho Core Domain Entities kèm Menu API, Upload file, và Availability Management**

---

## 2. Overview

Pine Drink là hệ thống order đồ uống online multi-brand/multi-branch. Tài liệu này mô tả chi tiết giải pháp xây dựng CRUD (Create, Read, Update, Delete) cho 6 domain entities chính: **Brand**, **Branch**, **Category**, **Product**, **ProductVariant**, **Topping**. Kèm theo đó là:

- **File upload service** (MinIO) cho phép upload ảnh sản phẩm, logo brand
- **Menu API** trả về cấu trúc menu đầy đủ theo brand và branch
- **Availability management** cho phép branch override giá và trạng thái sản phẩm/topping
- **Mapper pattern** chuyển đổi Entity ↔ Response/Request DTO

Tất cả entity đều kế thừa `BaseEntity` (ID dạng UUID string, tự động timestamp, soft-delete qua status field).

---

## 3. Purpose

- Cung cấp REST API CRUD hoàn chỉnh cho 6 domain entities
- Đảm bảo validation đồng bộ (unique code trong brand, price >= 0, ...)
- Xây dựng menu API phục vụ frontend hiển thị sản phẩm theo danh mục
- Cho phép từng branch quản lý availability riêng (giá bán, hết hàng, tạm ngưng)
- Upload và quản lý file ảnh qua MinIO
- Sử dụng mapper pattern tách biệt entity khỏi DTO
- Áp dụng `@PreAuthorize` phân quyền: ADMIN được CRUD, CUSTOMER/public chỉ được GET

---

## 4. Scope

### Trong phạm vi (In Scope)

- CRUD Brand (`/api/v1/brands`)
- CRUD Branch (`/api/v1/brands/{brandId}/branches`)
- CRUD Category (`/api/v1/brands/{brandId}/categories`)
- CRUD Product (`/api/v1/brands/{brandId}/products`)
- CRUD ProductVariant (`/api/v1/products/{productId}/variants`)
- CRUD Topping (`/api/v1/brands/{brandId}/toppings`)
- Upload image API (`/api/v1/uploads/image`)
- Brand Menu API (`GET /api/v1/brands/{brandId}/menu`)
- Branch Menu API (`GET /api/v1/branches/{branchId}/menu`)
- Branch Product Availability (`PATCH /api/v1/branches/{branchId}/product-availability`)
- Branch Topping Availability (`PATCH /api/v1/branches/{branchId}/topping-availability`)
- Mapper classes cho mỗi entity
- FileStorageService + UploadController (dùng MinIO đã có sẵn)
- Soft-delete (status = "DELETED") và active/inactive management
- Pagination cho list endpoints

### Ngoài phạm vi (Out of Scope)

- Product-topping mapping management (sẽ làm ở phase Product Config)
- Recipe/Ingredient management
- Bulk import/export
- Cache cho menu API (sẽ làm sau nếu cần)
- Audit logging chi tiết cho CRUD operations

---

## 5. Audience

| Đối tượng | Vai trò |
|-----------|---------|
| Developer Backend | Implement CRUD services, controllers, mappers |
| Developer Frontend | Tích hợp menu API, upload ảnh |
| QA / Tester | Viết test case cho CRUD + menu |
| Technical Leader | Review thiết kế |

---

## 6. Background

### 6.1 Vấn đề

Hệ thống Pine Drink cần quản lý danh mục sản phẩm đồ uống cho nhiều brand (thương hiệu) và branch (chi nhánh). Hiện tại đã có entity và repository cho Brand, Branch, Category, Product, ProductVariant, Topping nhưng chưa có service và controller. Cần xây dựng CRUD đầy đủ kèm menu API phục vụ frontend.

### 6.2 Entity Relationships

```
Brand (1) ──► Branch (n)
  │
  ├──► Category (n) ──► Product (n)
  │                           │
  │                           ├──► ProductVariant (n)
  │                           │
  │                           └──► BranchProductAvailability (n) ──► Branch (1)
  │
  └──► Topping (n) ──► BranchToppingAvailability (n) ──► Branch (1)
```

### 6.3 Ràng buộc

- Spring Boot 3.4.5 + Spring Security 6.x
- Entity dùng UUID string làm primary key (kế thừa `BaseEntity`)
- Repository đã có sẵn, cần bổ sung thêm method nếu thiếu
- MinIO đã được tích hợp qua `MinioFileStorageService`
- Response wrap trong `BaseResponse<T>` (success, message, data, timestamp)
- `@PreAuthorize` phân quyền: ADMIN = full CRUD, authenticated = GET, public = menu API

---

## 7. Requirements

### 7.1 Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| FR1 | CRUD Brand | Tạo/sửa/xóa brand, validate code unique, name required |
| FR2 | CRUD Branch | Tạo/sửa/xóa branch theo brand, unique code trong cùng brand |
| FR3 | CRUD Category | Tạo/sửa/xóa category theo brand, tự động display_order |
| FR4 | CRUD Product | Tạo/sửa/xóa product theo brand + category, validate base_price >= 0 |
| FR5 | CRUD ProductVariant | Tạo/sửa/xóa variant theo product, validate price_delta >= 0 |
| FR6 | CRUD Topping | Tạo/sửa/xóa topping theo brand, validate price >= 0 |
| FR7 | Upload Image | Upload file ảnh lên MinIO, trả về URL |
| FR8 | Brand Menu API | Lấy menu đầy đủ: categories → products → variants + toppings |
| FR9 | Branch Menu API | Lấy menu theo branch, override availability và sale_price |
| FR10 | Branch Product Availability | Set sản phẩm available/unavailable, sale_price theo branch |
| FR11 | Branch Topping Availability | Set topping available/unavailable theo branch |
| FR12 | Pagination | List endpoints hỗ trợ page, size, sort |

### 7.2 Non-Functional Requirements

| # | Yêu cầu | Mô tả |
|---|---------|-------|
| NFR1 | Response format | Nhất quán: `BaseResponse<T>` với success/message/data/timestamp |
| NFR2 | Validation | Validate đầu vào trước khi persist, trả về lỗi rõ ràng |
| NFR3 | Soft delete | Không xóa vật lý, set status = "DELETED" |
| NFR4 | Authorization | ADMIN mới được CREATE/UPDATE/DELETE. GET cho authenticated. Menu public |
| NFR5 | Error handling | Dùng `GlobalExceptionHandler` đã có, throw exception tương ứng |

### 7.3 Validation Rules

| Entity | Validation |
|--------|-----------|
| Brand | `code` unique global, not null, length 2-50. `name` not null, length 2-200. `phone` match pattern nếu có |
| Branch | `code` unique trong cùng brand, not null. `phone` match PATTERN_PHONE nếu có |
| Category | `name` not null. `displayOrder` >= 0 (auto-assign nếu null) |
| Product | `basePrice` >= 0 (BigDecimal). `name` not null. Category + Brand not null |
| ProductVariant | `variantName` not null. `priceDelta` >= 0 |
| Topping | `name` not null. `price` >= 0 (BigDecimal) |

---

## 8. Design / Giải pháp thiết kế

### 8.1 Kiến trúc tổng quan

```
┌─────────────────────────────────────────────────────────────────────┐
│                         Controller Layer                             │
│  BrandController  BranchController  CategoryController              │
│  ProductController  VariantController  ToppingController             │
│  MenuController  UploadController                                    │
└────────────────┬────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────────────┐
│                        Service Layer                                 │
│  BrandService  BranchService  CategoryService  ProductService       │
│  VariantService  ToppingService  FileStorageService                 │
│  (Interface + Impl pattern)                                          │
└────────────────┬────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────────────┐
│                        Mapper Layer                                  │
│  BrandMapper  BranchMapper  CategoryMapper                          │
│  ProductMapper  VariantMapper  ToppingMapper                        │
│  (Manual @Component, Entity ↔ Response/Request DTO)                 │
└────────────────┬────────────────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────────────────┐
│                      Repository Layer (đã có)                        │
│  BrandRepository  BranchRepository  CategoryRepository              │
│  ProductRepository  VariantRepository  ToppingRepository            │
│  + BranchProductAvailRepo  + BranchToppingAvailRepo                 │
└─────────────────────────────────────────────────────────────────────┘
```

### 8.2 Package Structure (files cần tạo)

```
com.hoandev.pinedrink
├── mapper/
│   ├── BrandMapper.java              ← NEW
│   ├── BranchMapper.java             ← NEW
│   ├── CategoryMapper.java           ← NEW
│   ├── ProductMapper.java            ← NEW
│   ├── ProductVariantMapper.java     ← NEW
│   └── ToppingMapper.java            ← NEW
├── service/
│   ├── BrandService.java             ← NEW
│   ├── BranchService.java            ← NEW
│   ├── CategoryService.java          ← NEW
│   ├── ProductService.java           ← NEW
│   ├── ProductVariantService.java    ← NEW
│   ├── ToppingService.java           ← NEW
│   └── impl/
│       ├── BrandServiceImpl.java     ← NEW
│       ├── BranchServiceImpl.java    ← NEW
│       ├── CategoryServiceImpl.java  ← NEW
│       ├── ProductServiceImpl.java   ← NEW
│       ├── ProductVariantServiceImpl.java ← NEW
│       └── ToppingServiceImpl.java   ← NEW
├── controller/
│   ├── BrandController.java          ← NEW
│   ├── BranchController.java         ← NEW
│   ├── CategoryController.java       ← NEW
│   ├── ProductController.java        ← NEW
│   ├── ProductVariantController.java ← NEW
│   ├── ToppingController.java        ← NEW
│   ├── MenuController.java           ← NEW
│   └── UploadController.java         ← NEW
└── entity/
    └── dto/
        ├── request/
        │   ├── BrandRequest.java            ← NEW
        │   ├── BranchRequest.java           ← NEW
        │   ├── CategoryRequest.java         ← NEW
        │   ├── ProductRequest.java          ← NEW
        │   ├── ProductVariantRequest.java   ← NEW
        │   ├── ToppingRequest.java          ← NEW
        │   ├── AvailabilityRequest.java     ← NEW
        │   └── BranchAvailRequest.java      ← NEW
        └── response/
            ├── BrandResponse.java           ← NEW
            ├── BranchResponse.java          ← NEW
            ├── CategoryResponse.java        ← NEW
            ├── ProductResponse.java         ← NEW
            ├── ProductVariantResponse.java  ← NEW
            ├── ToppingResponse.java         ← NEW
            ├── MenuResponse.java            ← NEW
            ├── MenuProductDto.java          ← NEW
            ├── MenuCategoryDto.java         ← NEW
            ├── BranchMenuResponse.java      ← NEW
            └── UploadResponse.java          ← NEW
```

### 8.3 DTO Design

#### Request DTOs

**BrandRequest**
```
code: String (not blank, 2-50 chars)
name: String (not blank, 2-200 chars)
legalName: String (optional)
taxCode: String (optional)
address: String (optional)
phone: String (optional, match PHONE_PATTERN)
email: String (optional, @Email)
timezone: String (optional, default "Asia/Ho_Chi_Minh")
```

**BranchRequest**
```
code: String (not blank)
name: String (not blank)
address: String (optional)
phone: String (optional, match PHONE_PATTERN)
email: String (optional, @Email)
latitude: BigDecimal (optional)
longitude: BigDecimal (optional)
timezone: String (optional, default "Asia/Ho_Chi_Minh")
supportsPickup: boolean (default true)
supportsDelivery: boolean (default false)
averagePreparationMinutes: int (default 15)
```

**CategoryRequest**
```
code: String (not blank)
name: String (not blank)
description: String (optional)
imageUrl: String (optional)
displayOrder: Integer (optional, auto-assign)
```

**ProductRequest**
```
code: String (not blank)
name: String (not blank)
description: String (optional)
imageUrl: String (optional)
basePrice: BigDecimal (not null, >= 0)
preparationMinutes: int (default 10)
isFeatured: boolean (default false)
isBestSeller: boolean (default false)
categoryId: String (not null)
```

**ProductVariantRequest**
```
variantCode: String (not blank)
variantName: String (not blank)
sizeLabel: String (optional)
priceDelta: BigDecimal (not null, >= 0, default 0)
displayOrder: Integer (optional, auto-assign)
```

**ToppingRequest**
```
code: String (not blank)
name: String (not blank)
price: BigDecimal (not null, >= 0)
imageUrl: String (optional)
```

#### Response DTOs

Mỗi entity response gồm tất cả field của entity (trừ quan hệ LAZY) + `id`, `status`, `createdAt`, `updatedAt`.

**BrandResponse**: id, code, name, legalName, taxCode, address, phone, email, timezone, status, createdAt, updatedAt

**BranchResponse**: id, code, name, address, phone, email, latitude, longitude, timezone, supportsPickup, supportsDelivery, averagePreparationMinutes, brandId, brandName, status, createdAt, updatedAt

**CategoryResponse**: id, code, name, description, imageUrl, displayOrder, brandId, status, createdAt, updatedAt

**ProductResponse**: id, code, name, description, imageUrl, basePrice, preparationMinutes, isFeatured, isBestSeller, brandId, categoryId, categoryName, status, createdAt, updatedAt

**ProductVariantResponse**: id, variantCode, variantName, sizeLabel, priceDelta, displayOrder, productId, status, createdAt, updatedAt

**ToppingResponse**: id, code, name, price, imageUrl, brandId, status, createdAt, updatedAt

### 8.4 Mapper Pattern

Mỗi entity có một mapper class riêng, là `@Component`, với 3 phương thức:

| Method | Mô tả |
|--------|-------|
| `toResponse(Entity)` | Entity → Response DTO |
| `toEntity(Request)` | Request DTO → Entity (chỉ set các field từ request) |
| `updateEntity(Entity, Request)` | Cập nhật entity từ request DTO (dùng cho PUT/PATCH) |

Không dùng MapStruct để giữ đơn giản và dễ debug. Entity field được set thủ công.

### 8.5 Service Layer Pattern

Mỗi service interface có các method CRUD + business logic:

```
interface BrandService {
    List<BrandResponse> getAll(Pageable pageable);
    BrandResponse getById(String id);
    BrandResponse getByCode(String code);
    BrandResponse create(BrandRequest request);
    BrandResponse update(String id, BrandRequest request);
    void delete(String id);               // soft delete
    BrandResponse updateStatus(String id, String status);  // active/inactive
}

interface BranchService {
    List<BranchResponse> getByBrandId(String brandId, Pageable pageable);
    BranchResponse getById(String id);
    BranchResponse create(String brandId, BranchRequest request);
    BranchResponse update(String id, BranchRequest request);
    void delete(String id);
    BranchResponse updateStatus(String id, String status);
}

// Tương tự cho CategoryService, ProductService, ProductVariantService, ToppingService
```

**Implementation pattern**:
- `@Transactional` cho create/update/delete
- Validate unique constraints trước khi save
- Throw `ResourceNotFoundException` khi entity không tồn tại
- Throw `DuplicateResourceException` khi vi phạm unique
- Throw `BadRequestException` khi validation business fail
- Soft delete: set `status = Constants.STATUS_DELETED`

---

## 9. Details / Chi tiết triển khai

### 9.1 Brand CRUD

**BrandMapper** (`mapper/BrandMapper.java`)
- `toResponse(Brand)`: map all fields từ entity
- `toEntity(BrandRequest)`: tạo Brand mới với code, name, legalName, taxCode, address, phone, email, timezone
- `updateEntity(Brand, BrandRequest)`: cập nhật name, legalName, taxCode, address, phone, email, timezone

**BrandServiceImpl** (`service/impl/BrandServiceImpl.java`)
- `create(BrandRequest)`: 
  1. Check `code` unique → nếu tồn tại throw `DuplicateResourceException`
  2. `brandMapper.toEntity(request)`
  3. Set `status = Constants.STATUS_ACTIVE`
  4. `brandRepository.save(brand)`
  5. Return `brandMapper.toResponse(saved)`
- `update(String id, BrandRequest)`:
  1. `findById(id)` → throw `ResourceNotFoundException`
  2. Check code unique nếu code thay đổi
  3. `brandMapper.updateEntity(brand, request)`
  4. Save + return response
- `delete(String id)`:
  1. Tìm brand
  2. Set `status = Constants.STATUS_DELETED`
  3. Cascade: set status DELETED cho tất cả branch, category, product thuộc brand

**BrandController** (`controller/BrandController.java`)
- Base URL: `/api/v1/brands`
- `GET /api/v1/brands` → `getAll(pageable)` → `PageResponse<BrandResponse>`
- `GET /api/v1/brands/{id}` → `getById(id)` → `BaseResponse<BrandResponse>`
- `POST /api/v1/brands` → `create(request)` → `201 + BaseResponse<BrandResponse>`
- `PUT /api/v1/brands/{id}` → `update(id, request)` → `BaseResponse<BrandResponse>`
- `DELETE /api/v1/brands/{id}` → `delete(id)` → `BaseResponse<Void>`
- `PATCH /api/v1/brands/{id}/status` → `updateStatus(id, status)` → `BaseResponse<BrandResponse>`
- `@PreAuthorize("hasRole('ADMIN')")` trên class (trừ GET endpoints)

### 9.2 Branch CRUD

**BranchMapper** (`mapper/BranchMapper.java`)
- `toResponse(Branch)`: map fields + `brandId` (từ brand.getId()), `brandName` (từ brand.getName())
- `toEntity(BranchRequest)`: tạo Branch với các field từ request
- `updateEntity(Branch, BranchRequest)`: cập nhật các field

**BranchServiceImpl** (`service/impl/BranchServiceImpl.java`)
- `create(String brandId, BranchRequest)`:
  1. `brandRepository.findById(brandId)` → throw nếu không tìm thấy
  2. Check `code` unique trong cùng brand: `branchRepository.findByBrandIdAndCode(brandId, code)` → throw nếu tồn tại
  3. `branchMapper.toEntity(request)`
  4. Set brand, status ACTIVE
  5. Save + return response
- Cần bổ sung method `findByBrandIdAndCode` vào BranchRepository:
  ```java
  Optional<Branch> findByBrandIdAndCode(String brandId, String code);
  boolean existsByBrandIdAndCode(String brandId, String code);
  ```

### 9.3 Category CRUD

**CategoryMapper** (`mapper/CategoryMapper.java`)

**CategoryServiceImpl** (`service/impl/CategoryServiceImpl.java`)
- `create(String brandId, CategoryRequest)`:
  1. Validate brand tồn tại
  2. Auto-assign `displayOrder` nếu null:
     ```java
     if (request.getDisplayOrder() == null) {
         Integer maxOrder = categoryRepository.findMaxDisplayOrderByBrandId(brandId);
         request.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 1);
     }
     ```
  3. Validate code unique trong brand
  4. Set brand, status ACTIVE → save
- Cần bổ sung method vào **CategoryRepository**:
  ```java
  Optional<Category> findByBrandIdAndCode(String brandId, String code);
  @Query("SELECT MAX(c.displayOrder) FROM Category c WHERE c.brand.id = :brandId")
  Integer findMaxDisplayOrderByBrandId(@Param("brandId") String brandId);
  ```

### 9.4 Product CRUD

**ProductMapper** (`mapper/ProductMapper.java`)
- `toResponse(Product)`: map fields + `categoryId`, `categoryName`, `brandId`

**ProductServiceImpl** (`service/impl/ProductServiceImpl.java`)
- `create(String brandId, ProductRequest)`:
  1. Validate brand tồn tại
  2. Validate category tồn tại và thuộc brand
  3. `basePrice` >= 0
  4. Check code unique trong brand
  5. Set brand, category, status ACTIVE → save
- `getActiveByCategoryId(String categoryId)`: lấy product active theo category

### 9.5 ProductVariant CRUD

**ProductVariantMapper** (`mapper/ProductVariantMapper.java`)

**ProductVariantServiceImpl** (`service/impl/ProductVariantServiceImpl.java`)
- `create(String productId, ProductVariantRequest)`:
  1. Validate product tồn tại
  2. `priceDelta` >= 0
  3. Set product, status ACTIVE → save

### 9.6 Topping CRUD

**ToppingMapper** (`mapper/ToppingMapper.java`)

**ToppingServiceImpl** (`service/impl/ToppingServiceImpl.java`)
- `create(String brandId, ToppingRequest)`:
  1. Validate brand tồn tại
  2. `price` >= 0
  3. Set brand, status ACTIVE → save

### 9.7 Upload Image

**UploadController** (`controller/UploadController.java`)

```java
@RestController
@RequestMapping("/api/v1/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @PostMapping("/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<FileUploadResponse>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        fileStorageService.validateFile(file);
        String fileUrl = fileStorageService.uploadFile(file, "images", FileVisibility.PUBLIC);
        FileUploadResponse response = FileUploadResponse.builder()
                .fileUrl(fileUrl)
                .originalFilename(file.getOriginalFilename())
                .fileSize(file.getSize())
                .contentType(file.getContentType())
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "File uploaded successfully"));
    }
}
```

**Lưu ý**: Dùng `FileStorageService` đã có sẵn (`MinioFileStorageService`). File được upload với `FileVisibility.PUBLIC` để có thể truy cập trực tiếp qua URL.

### 9.8 Menu API

#### Brand Menu — GET `/api/v1/brands/{brandId}/menu`

**Response structure**:
```json
{
    "success": true,
    "data": {
        "brandId": "uuid",
        "brandName": "Pine Coffee",
        "categories": [
            {
                "category": { /* CategoryResponse */ },
                "products": [
                    {
                        "product": { /* ProductResponse */ },
                        "variants": [ /* ProductVariantResponse */ ]
                    }
                ]
            }
        ],
        "toppings": [ /* ToppingResponse */ ]
    }
}
```

**MenuController** (`controller/MenuController.java`)
- Public endpoint, không cần auth
- `GET /api/v1/brands/{brandId}/menu`:
  1. Lấy categories active của brand (order by displayOrder)
  2. Với mỗi category, lấy products active
  3. Với mỗi product, lấy variants active (order by displayOrder)
  4. Lấy toppings active của brand
  5. Build MenuResponse

#### Branch Menu — GET `/api/v1/branches/{branchId}/menu`

**Response structure**:
```json
{
    "success": true,
    "data": {
        "branchId": "uuid",
        "branchName": "Pine Coffee - Nguyễn Huệ",
        "categories": [ /* MenuCategoryDto */ ],
        "productAvailabilities": [
            {
                "productId": "uuid",
                "isAvailable": true,
                "salePrice": 35000,
                "soldOutReason": null
            }
        ],
        "toppingAvailabilities": [
            {
                "toppingId": "uuid",
                "isAvailable": false,
                "soldOutReason": "Hết nguyên liệu"
            }
        ]
    }
}
```

1. Lấy branch info
2. Lấy brand menu từ brand của branch
3. Lấy `BranchProductAvailability` list
4. Lấy `BranchToppingAvailability` list
5. Ghép vào response (frontend sẽ tự override)

### 9.9 Branch Availability Management

#### BranchProductAvailability

**Endpoint**: `PATCH /api/v1/branches/{branchId}/product-availability`

**Request body**:
```json
{
    "productId": "uuid",
    "isAvailable": true,
    "salePrice": 35000,
    "soldOutReason": null
}
```

- Nếu record chưa tồn tại → create mới
- Nếu đã tồn tại → update
- `salePrice` có thể null (dùng base_price của product)

**Service logic**:
```java
@Transactional
public BranchProductAvailabilityResponse setProductAvailability(
        String branchId, AvailabilityRequest request) {
    Branch branch = branchRepository.findById(branchId)
            .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

    BranchProductAvailability availability = branchProductAvailabilityRepository
            .findByBranchIdAndProductId(branchId, request.getProductId())
            .orElseGet(() -> {
                BranchProductAvailability newAvail = new BranchProductAvailability();
                newAvail.setBranch(branch);
                newAvail.setProduct(product);
                return newAvail;
            });

    availability.setAvailable(request.isAvailable());
    availability.setSalePrice(request.getSalePrice());
    availability.setSoldOutReason(request.getSoldOutReason());

    BranchProductAvailability saved = branchProductAvailabilityRepository.save(availability);
    return branchProductAvailabilityMapper.toResponse(saved);
}
```

#### BranchToppingAvailability

**Endpoint**: `PATCH /api/v1/branches/{branchId}/topping-availability`

Tương tự pattern trên.

### 9.10 Repository Bổ Sung

Cần bổ sung các method sau vào repository hiện có:

**BranchRepository**:
```java
Optional<Branch> findByBrandIdAndCode(String brandId, String code);
boolean existsByBrandIdAndCode(String brandId, String code);
List<Branch> findByBrandId(String brandId);
```

**ProductRepository**:
```java
Optional<Product> findByBrandIdAndCode(String brandId, String code);
boolean existsByBrandIdAndCode(String brandId, String code);
List<Product> findByCategoryIdAndStatus(String categoryId, String status);
```

**CategoryRepository**:
```java
Optional<Category> findByBrandIdAndCode(String brandId, String code);
boolean existsByBrandIdAndCode(String brandId, String code);
@Query("SELECT MAX(c.displayOrder) FROM Category c WHERE c.brand.id = :brandId")
Integer findMaxDisplayOrderByBrandId(@Param("brandId") String brandId);
```

**ProductVariantRepository**:
```java
Optional<ProductVariant> findByProductIdAndVariantCode(String productId, String variantCode);
List<ProductVariant> findByProductId(String productId);
```

**ToppingRepository**:
```java
Optional<Topping> findByBrandIdAndCode(String brandId, String code);
boolean existsByBrandIdAndCode(String brandId, String code);
```

**BranchToppingAvailabilityRepository**:
```java
Optional<BranchToppingAvailability> findByBranchIdAndToppingId(String branchId, String toppingId);
```

---

## 10. API Endpoints

| Method | Endpoint | Auth | Mô tả |
|--------|----------|------|-------|
| GET | `/api/v1/brands` | Authenticated | Danh sách brands (phân trang) |
| GET | `/api/v1/brands/{id}` | Authenticated | Chi tiết brand |
| POST | `/api/v1/brands` | ADMIN | Tạo brand mới |
| PUT | `/api/v1/brands/{id}` | ADMIN | Cập nhật brand |
| DELETE | `/api/v1/brands/{id}` | ADMIN | Xóa mềm brand |
| PATCH | `/api/v1/brands/{id}/status` | ADMIN | Active/Inactive brand |
| GET | `/api/v1/brands/{brandId}/branches` | Authenticated | Danh sách branch theo brand |
| GET | `/api/v1/branches/{id}` | Authenticated | Chi tiết branch |
| POST | `/api/v1/brands/{brandId}/branches` | ADMIN | Tạo branch mới |
| PUT | `/api/v1/branches/{id}` | ADMIN | Cập nhật branch |
| DELETE | `/api/v1/branches/{id}` | ADMIN | Xóa mềm branch |
| PATCH | `/api/v1/branches/{id}/status` | ADMIN | Active/Inactive branch |
| GET | `/api/v1/brands/{brandId}/categories` | Authenticated | Danh sách category theo brand |
| GET | `/api/v1/categories/{id}` | Authenticated | Chi tiết category |
| POST | `/api/v1/brands/{brandId}/categories` | ADMIN | Tạo category mới |
| PUT | `/api/v1/categories/{id}` | ADMIN | Cập nhật category |
| DELETE | `/api/v1/categories/{id}` | ADMIN | Xóa mềm category |
| GET | `/api/v1/brands/{brandId}/products` | Authenticated | Danh sách product theo brand |
| GET | `/api/v1/products/{id}` | Authenticated | Chi tiết product |
| POST | `/api/v1/brands/{brandId}/products` | ADMIN | Tạo product mới |
| PUT | `/api/v1/products/{id}` | ADMIN | Cập nhật product |
| DELETE | `/api/v1/products/{id}` | ADMIN | Xóa mềm product |
| GET | `/api/v1/products/{productId}/variants` | Authenticated | Danh sách variant theo product |
| GET | `/api/v1/variants/{id}` | Authenticated | Chi tiết variant |
| POST | `/api/v1/products/{productId}/variants` | ADMIN | Tạo variant mới |
| PUT | `/api/v1/variants/{id}` | ADMIN | Cập nhật variant |
| DELETE | `/api/v1/variants/{id}` | ADMIN | Xóa mềm variant |
| GET | `/api/v1/brands/{brandId}/toppings` | Authenticated | Danh sách topping theo brand |
| GET | `/api/v1/toppings/{id}` | Authenticated | Chi tiết topping |
| POST | `/api/v1/brands/{brandId}/toppings` | ADMIN | Tạo topping mới |
| PUT | `/api/v1/toppings/{id}` | ADMIN | Cập nhật topping |
| DELETE | `/api/v1/toppings/{id}` | ADMIN | Xóa mềm topping |
| GET | `/api/v1/brands/{brandId}/menu` | Public | Brand menu (categories + products + variants + toppings) |
| GET | `/api/v1/branches/{branchId}/menu` | Public | Branch menu với availability override |
| POST | `/api/v1/uploads/image` | ADMIN | Upload file ảnh |
| PATCH | `/api/v1/branches/{branchId}/product-availability` | ADMIN | Set product availability cho branch |
| PATCH | `/api/v1/branches/{branchId}/topping-availability` | ADMIN | Set topping availability cho branch |

### 10.1 Pagination Pattern

Tất cả list endpoint đều hỗ trợ pagination qua Spring Pageable:

```java
@GetMapping
public ResponseEntity<BaseResponse<PageResponse<BrandResponse>>> getAll(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
        Pageable pageable) {
    Page<BrandResponse> page = brandService.getAll(pageable);
    return ResponseEntity.ok(BaseResponse.success(PageResponse.from(page)));
}
```

Query parameters:
- `?page=0&size=20&sort=createdAt,desc`
- `?page=0&size=10&sort=name,asc`

### 10.2 Error Response Format

```json
// Success
{
    "success": true,
    "message": "Brand created successfully",
    "data": { /* BrandResponse */ },
    "timestamp": "2026-05-29T10:00:00Z"
}

// Validation error (400)
{
    "success": false,
    "errorCode": "COM_001",
    "message": "Validation failed",
    "fieldErrors": [
        { "field": "code", "message": "Mã brand không được để trống" }
    ],
    "timestamp": "2026-05-29T10:00:00Z"
}

// Not found (404)
{
    "success": false,
    "errorCode": "COM_005",
    "message": "Brand not found with id: xxx",
    "timestamp": "2026-05-29T10:00:00Z"
}

// Duplicate (409)
{
    "success": false,
    "errorCode": "COM_001",
    "message": "Brand code already exists: PINE",
    "timestamp": "2026-05-29T10:00:00Z"
}
```

---

## 11. Security / Phân quyền

### 11.1 Authorization Matrix

| Endpoint Group | CUSTOMER | STAFF | ADMIN | Public |
|----------------|----------|-------|-------|--------|
| GET `/api/v1/brands/**` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/v1/branches/**` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/v1/categories/**` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/v1/products/**` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/v1/variants/**` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/v1/toppings/**` | ✅ | ✅ | ✅ | ❌ |
| POST/PUT/DELETE `/api/v1/**` | ❌ | ❌ | ✅ | ❌ |
| GET `/api/v1/brands/{id}/menu` | ✅ | ✅ | ✅ | ✅ |
| GET `/api/v1/branches/{id}/menu` | ✅ | ✅ | ✅ | ✅ |
| POST `/api/v1/uploads/image` | ❌ | ❌ | ✅ | ❌ |
| PATCH `*/product-availability` | ❌ | ❌ | ✅ | ❌ |
| PATCH `*/topping-availability` | ❌ | ❌ | ✅ | ❌ |

### 11.2 SecurityConfig Cập Nhật

Cần cập nhật `SecurityConfig.java` để cho phép public endpoints cho menu:

```java
// Public endpoints (không cần auth)
requestMatchers(
    "/api/v1/brands/*/menu",
    "/api/v1/branches/*/menu"
).permitAll()
```

Các endpoint GET `/api/v1/brands/**` hiện đang public trong config — cần review:
- Nếu để public → tất cả đều xem được brand/branch/product
- Nếu chỉ để authenticated → cần login mới xem được

**Recommendation**: Menu API public, CRUD endpoints yêu cầu authenticated.

---

## 12. Risks / Rủi ro

| # | Rủi ro | Tác động | Khả năng | Giải pháp |
|---|--------|----------|----------|-----------|
| R1 | Cascade soft delete brand → mất hết branch/product | Mất dữ liệu liên quan | Thấp | Warn khi delete brand có branch active. Cho phép force delete |
| R2 | Unique constraint violation race condition (2 request cùng lúc) | Duplicate code | Thấp | DB unique constraint làm safeguard cuối cùng. Bắt `DataIntegrityViolationException` trong service |
| R3 | N+1 query trên menu API (categories → products → variants) | Hiệu năng kém | Cao | Dùng `@EntityGraph` hoặc fetch join JPQL. Batch fetch nếu cần |
| R4 | File upload không giới hạn kích thước | Server disk đầy | Trung bình | `MinioFileStorageService.validateFile()` đã check size + extension |
| R5 | Menu API trả về quá nhiều dữ liệu | Response lớn, chậm | Trung bình | Thêm pagination cho products trong category. Cân nhắc cache Redis |

---

## 13. Testing / Kiểm thử

### 13.1 Unit Tests (JUnit 5 + Mockito)

#### Mapper Tests

| Test case | Mô tả |
|-----------|-------|
| `BrandMapperTest.toResponse` | Entity → Response: tất cả field đúng |
| `BrandMapperTest.toEntity` | Request → Entity: field được map |
| `BrandMapperTest.updateEntity` | Update entity: field thay đổi, field không thay đổi |

#### Service Tests

| Test case | Mô tả |
|-----------|-------|
| `BrandServiceTest.createSuccess` | Tạo brand hợp lệ → thành công |
| `BrandServiceTest.createDuplicateCode` | Code đã tồn tại → DuplicateResourceException |
| `BrandServiceTest.getByIdNotFound` | Id không tồn tại → ResourceNotFoundException |
| `BrandServiceTest.deleteThenGet` | Soft delete → get lại → status = DELETED |
| `BranchServiceTest.createWithUniqueCodeInBrand` | 2 brand có cùng code branch → OK |
| `BranchServiceTest.createWithDuplicateCodeInSameBrand` | Cùng brand, trùng code branch → Exception |
| `CategoryServiceTest.createWithAutoDisplayOrder` | Không truyền displayOrder → tự động tăng |
| `ProductServiceTest.createWithNegativePrice` | basePrice < 0 → BadRequestException |
| `ProductVariantServiceTest.createWithNegativeDelta` | priceDelta < 0 → BadRequestException |
| `ToppingServiceTest.createWithNegativePrice` | price < 0 → BadRequestException |
| `MenuServiceTest.getBrandMenu` | Trả về đúng cấu trúc categories → products → variants + toppings |
| `BranchAvailabilityServiceTest.setProductAvailability` | Tạo mới / cập nhật availability |

### 13.2 Integration Tests

| Test case | Mô tả |
|-----------|-------|
| `BrandControllerTest.fullCrud` | POST → GET → PUT → DELETE → GET (404) |
| `BranchControllerTest.crudUnderBrand` | Tạo brand → tạo branch → list theo brand |
| `MenuControllerTest.getBrandMenu_returnsCorrectStructure` | GET menu → kiểm tra JSON path |
| `MenuControllerTest.getBranchMenu_withAvailability` | Set availability → GET branch menu → check override |
| `UploadControllerTest.uploadImage` | Upload file → 201 + URL |
| `AuthorizationTest.adminCanCrud_customerReadOnly` | ADMIN token → POST thành công. CUSTOMER token → 403 |

### 13.3 Test Data

```sql
-- Brand test
INSERT INTO ce_brand (id, code, name, timezone, status)
VALUES ('brand-1', 'PINE', 'Pine Coffee', 'Asia/Ho_Chi_Minh', 'ACTIVE');

-- Branch test
INSERT INTO ce_branch (id, brand_id, code, name, phone, timezone, status)
VALUES ('branch-1', 'brand-1', 'CN-01', 'Pine Coffee - Nguyễn Huệ',
        '0900000001', 'Asia/Ho_Chi_Minh', 'ACTIVE');

-- Category test
INSERT INTO pr_category (id, brand_id, code, name, display_order, status)
VALUES ('cat-1', 'brand-1', 'COFFEE', 'Cà phê', 1, 'ACTIVE');
INSERT INTO pr_category (id, brand_id, code, name, display_order, status)
VALUES ('cat-2', 'brand-1', 'TEA', 'Trà', 2, 'ACTIVE');

-- Product test
INSERT INTO pr_product (id, brand_id, category_id, code, name, base_price, status)
VALUES ('prod-1', 'brand-1', 'cat-1', 'ESPRESSO', 'Espresso', 29000, 'ACTIVE');

-- Variant test
INSERT INTO pr_product_variant (id, product_id, variant_code, variant_name, price_delta, display_order, status)
VALUES ('var-1', 'prod-1', 'S', 'Nhỏ', 0, 1, 'ACTIVE');
INSERT INTO pr_product_variant (id, product_id, variant_code, variant_name, price_delta, display_order, status)
VALUES ('var-2', 'prod-1', 'L', 'Lớn', 5000, 2, 'ACTIVE');

-- Topping test
INSERT INTO pr_topping (id, brand_id, code, name, price, status)
VALUES ('top-1', 'brand-1', 'TRÂN_CHÂU', 'Trân châu', 5000, 'ACTIVE');

-- Branch Product Availability test
INSERT INTO mn_branch_product_availability (id, branch_id, product_id, is_available, sale_price)
VALUES ('bpa-1', 'branch-1', 'prod-1', true, 25000);
```

---

## 14. Implementation Order / Thứ tự triển khai

| Step | Task | Dependency |
|------|------|------------|
| 1 | Tạo Request/Response DTOs (BrandRequest → ToppingResponse) | Không |
| 2 | Tạo Mappers (BrandMapper → ToppingMapper) | Step 1 |
| 3 | Bổ sung repository methods | Step 1 |
| 4 | Tạo Services (BrandService → ToppingService) | Step 2 + 3 |
| 5 | Tạo Controllers (BrandController → ToppingController) | Step 4 |
| 6 | UploadController | Step 4 |
| 7 | MenuController + MenuResponse DTOs | Step 5 |
| 8 | Branch Availability services + PATCH endpoints | Step 4 |
| 9 | Update SecurityConfig nếu cần | Step 5 + 7 |
| 10 | Test từng CRUD endpoint | Step 5 |
| 11 | Test menu API | Step 7 |
| 12 | Test upload file | Step 6 |
| 13 | Test availability | Step 8 |

---

## 15. Appendix / Phụ lục

### 15.1 File Upload Response

```json
{
    "success": true,
    "message": "File uploaded successfully",
    "data": {
        "fileUrl": "http://minio:9000/pine-public/images/uuid.jpg",
        "originalFilename": "product.jpg",
        "fileSize": 102400,
        "contentType": "image/jpeg"
    },
    "timestamp": "2026-05-29T10:00:00Z"
}
```

### 15.2 Controller Code Pattern

```java
@RestController
@RequestMapping("/api/v1/brands/{brandId}/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    private final BranchService branchService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAll(
            @PathVariable String brandId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<BranchResponse> page = branchService.getByBrandId(brandId, pageable);
        return ResponseEntity.ok(BaseResponse.success(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BaseResponse<BranchResponse>> getById(@PathVariable String id) {
        BranchResponse response = branchService.getById(id);
        return ResponseEntity.ok(BaseResponse.success(response));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> create(
            @PathVariable String brandId,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse response = branchService.create(brandId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Branch created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody BranchRequest request) {
        BranchResponse response = branchService.update(id, request);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> delete(@PathVariable String id) {
        branchService.delete(id);
        return ResponseEntity.ok(BaseResponse.success(null, "Branch deleted successfully"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<BranchResponse>> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        BranchResponse response = branchService.updateStatus(id, status);
        return ResponseEntity.ok(BaseResponse.success(response, "Branch status updated"));
    }
}
```

### 15.3 Service Impl Pattern

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponse> getAll(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(brandMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getById(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        return brandMapper.toResponse(brand);
    }

    @Override
    public BrandResponse create(BrandRequest request) {
        if (brandRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Brand code already exists: " + request.getCode());
        }
        Brand brand = brandMapper.toEntity(request);
        brand.setStatus(Constants.STATUS_ACTIVE);
        Brand saved = brandRepository.save(brand);
        log.info("Brand created: code={}, id={}", saved.getCode(), saved.getId());
        return brandMapper.toResponse(saved);
    }

    @Override
    public BrandResponse update(String id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        if (!brand.getCode().equals(request.getCode())
                && brandRepository.findByCode(request.getCode()).isPresent()) {
            throw new DuplicateResourceException("Brand code already exists: " + request.getCode());
        }
        brandMapper.updateEntity(brand, request);
        Brand saved = brandRepository.save(brand);
        log.info("Brand updated: id={}", saved.getId());
        return brandMapper.toResponse(saved);
    }

    @Override
    public void delete(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brand.setStatus(Constants.STATUS_DELETED);
        brandRepository.save(brand);
        log.info("Brand soft-deleted: id={}", id);
    }

    @Override
    public BrandResponse updateStatus(String id, String status) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brand.setStatus(status);
        Brand saved = brandRepository.save(brand);
        return brandMapper.toResponse(saved);
    }
}
```

### 15.4 Critical Implementation Notes

1. **Branch unique code**: `(brand_id, code)` là unique constraint — check trùng trong service trước khi save
2. **Product base_price**: Dùng `BigDecimal.compareTo(BigDecimal.ZERO)` thay vì `signum()` — tránh lỗi so sánh BigDecimal
3. **Display order**: Category và ProductVariant auto-assign display_order dựa trên max hiện tại + 1
4. **Menu N+1**: Dùng `@EntityGraph(attributePaths = {"category", "brand"})` trên ProductRepository để fetch join, tránh N+1
5. **Soft delete cascade**: Khi delete brand, cần cân nhắc cascade xuống branch/category/product. Hiện tại để manual — không cascade tự động
6. **Branch availability sale_price**: Nếu `salePrice` null → frontend dùng `basePrice` của product

### 15.5 References

- [Spring Data JPA Pagination](https://docs.spring.io/spring-data/jpa/reference/repositories/core-extensions.html)
- [Spring Security Method Security](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html)
- [MinIO Java SDK](https://min.io/docs/minio/linux/developers/java/minio-java.html)
- [BaseEntity & Existing Architecture](./plan-01-auth-security-tdd.md)
