# Plan 2 — Core Domain CRUD

## Mục tiêu
Xây dựng CRUD đầy đủ cho các domain entities: Brand, Branch, Category, Product, ProductVariant, Topping. Kèm theo upload file, menu API, và availability management.

## Files cần tạo/sửa

### Mappers
- `mapper/BrandMapper.java`
- `mapper/BranchMapper.java`
- `mapper/ProductMapper.java`
- `mapper/CategoryMapper.java`
- `mapper/ToppingMapper.java`

### Services
- `service/BrandService.java` + `service/impl/BrandServiceImpl.java`
- `service/BranchService.java` + `service/impl/BranchServiceImpl.java`
- `service/CategoryService.java` + `service/impl/CategoryServiceImpl.java`
- `service/ProductService.java` + `service/impl/ProductServiceImpl.java`
- `service/ProductVariantService.java` + `service/impl/ProductVariantServiceImpl.java`
- `service/ToppingService.java` + `service/impl/ToppingServiceImpl.java`
- `service/FileStorageService.java`

### Controllers
- `controller/BrandController.java`
- `controller/BranchController.java`
- `controller/CategoryController.java`
- `controller/ProductController.java`
- `controller/ToppingController.java`
- `controller/UploadController.java`

### Config
- `configuration/FileStorageConfig.java` — cấu hình local/MinIO storage
- `configuration/MinIOConfig.java` — nếu dùng MinIO

## Chi tiết Implementation

### Validation Rules

| Entity | Validation |
|--------|-----------|
| Brand | `code` unique, không null, 2-20 ký tự. `name` không null, 2-100 ký tự |
| Branch | `code` unique trong cùng brand, không null. `phone` match PATTERN_PHONE |
| Category | `name` không null. `display_order` >= 0 |
| Product | `base_price` >= 0. `name` không null. `category` không null |
| ProductVariant | `name` không null. `additional_price` >= 0 |
| Topping | `name` không null. `price` >= 0 |

### Mapper Pattern

Entity ↔ Response DTO, Request DTO → Entity. Dùng MapStruct hoặc manual:

```java
@Component
public class BrandMapper {

    public BrandResponse toResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .code(brand.getCode())
                .name(brand.getName())
                .description(brand.getDescription())
                .logoUrl(brand.getLogoUrl())
                .status(brand.getStatus())
                .createdAt(brand.getCreatedAt())
                .updatedAt(brand.getUpdatedAt())
                .build();
    }

    public Brand toEntity(BrandRequest request) {
        return Brand.builder()
                .code(request.getCode())
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .build();
    }

    public void updateEntity(@NonNull Brand brand, @NonNull BrandRequest request) {
        brand.setName(request.getName());
        brand.setDescription(request.getDescription());
        brand.setLogoUrl(request.getLogoUrl());
    }
}
```

### ProductService.createProduct()

```java
@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BrandRepository brandRepository;
    @Autowired private ProductMapper productMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        // Validate category tồn tại
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

        // Validate brand tồn tại
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Brand not found with id: " + request.getBrandId()));

        // Validate base_price >= 0
        if (request.getBasePrice() != null && request.getBasePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("base_price must be >= 0");
        }

        // Kiểm tra unique (code + brand)
        if (request.getCode() != null &&
                productRepository.existsByCodeAndBrandId(request.getCode(), request.getBrandId())) {
            throw new DuplicateResourceException(
                    "Product code already exists in this brand: " + request.getCode());
        }

        Product product = productMapper.toEntity(request);
        product.setCategory(category);
        product.setBrand(brand);
        product.setStatus(Constants.STATUS_ACTIVE);

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }
}
```

### CategoryService với display_order

```java
@Override
public CategoryResponse createCategory(CategoryRequest request) {
    Brand brand = brandRepository.findById(request.getBrandId())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Brand not found with id: " + request.getBrandId()));

    // Auto-assign display_order nếu không được cung cấp
    if (request.getDisplayOrder() == null) {
        Integer maxOrder = categoryRepository.findMaxDisplayOrderByBrandId(request.getBrandId());
        request.setDisplayOrder(maxOrder != null ? maxOrder + 1 : 1);
    }

    Category category = categoryMapper.toEntity(request);
    category.setBrand(brand);
    category.setStatus(Constants.STATUS_ACTIVE);

    Category saved = categoryRepository.save(category);
    return categoryMapper.toResponse(saved);
}
```

### Menu API — GET /brands/{brandId}/menu

```java
@RestController
@RequestMapping("/api/v1/brands/{brandId}")
public class MenuController {

    @Autowired private CategoryService categoryService;
    @Autowired private ProductService productService;
    @Autowired private ToppingService toppingService;

    @GetMapping("/menu")
    public MenuResponse getBrandMenu(@PathVariable Long brandId) {
        List<CategoryResponse> categories = categoryService.getActiveByBrandId(brandId);

        List<MenuCategoryDto> categoryDtos = categories.stream().map(cat -> {
            List<ProductResponse> products = productService.getActiveByCategoryId(cat.getId());

            List<MenuProductDto> productDtos = products.stream().map(product -> {
                List<ProductVariantResponse> variants =
                        productService.getVariantsByProductId(product.getId());
                return MenuProductDto.builder()
                        .product(product)
                        .variants(variants)
                        .build();
            }).collect(Collectors.toList());

            return MenuCategoryDto.builder()
                    .category(cat)
                    .products(productDtos)
                    .build();
        }).collect(Collectors.toList());

        List<ToppingResponse> toppings = toppingService.getActiveByBrandId(brandId);

        return MenuResponse.builder()
                .brandId(brandId)
                .categories(categoryDtos)
                .toppings(toppings)
                .build();
    }
}
```

### Branch Menu — GET /branches/{branchId}/menu

```java
@GetMapping("/branches/{branchId}/menu")
public BranchMenuResponse getBranchMenu(@PathVariable Long branchId) {
    Branch branch = branchService.getById(branchId);

    // Lấy menu gốc của brand
    MenuResponse brandMenu = getBrandMenu(branch.getBrand().getId());

    // Override availability và sale_price từ BranchProductAvailability
    List<BranchProductAvailability> productAvailabilities =
            branchProductAvailabilityService.getByBranchId(branchId);

    List<BranchToppingAvailability> toppingAvailabilities =
            branchToppingAvailabilityService.getByBranchId(branchId);

    // Map availability vào response
    return BranchMenuResponse.builder()
            .branchId(branchId)
            .branchName(branch.getName())
            .categories(brandMenu.getCategories())
            .productAvailabilities(productAvailabilities)
            .toppingAvailabilities(toppingAvailabilities)
            .build();
}
```

### UploadController

```java
@RestController
@RequestMapping("/api/v1/uploads")
public class UploadController {

    @Autowired private FileStorageService fileStorageService;

    @PostMapping("/image")
    public UploadResponse uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.storeFile(file, "images");
        return UploadResponse.builder()
                .url(url)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .build();
    }
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| CRUD | /api/v1/brands | Brand management |
| CRUD | /api/v1/brands/{brandId}/branches | Branch management |
| CRUD | /api/v1/categories | Category management |
| CRUD | /api/v1/products | Product management |
| CRUD | /api/v1/variants | ProductVariant management |
| CRUD | /api/v1/toppings | Topping management |
| GET | /api/v1/brands/{brandId}/menu | Brand menu (categories + products + variants + toppings) |
| GET | /api/v1/branches/{branchId}/menu | Branch menu với availability + sale_price |
| POST | /api/v1/uploads/image | Upload ảnh |
| PATCH | /api/v1/branches/{branchId}/product-availability | Set product availability |
| PATCH | /api/v1/branches/{branchId}/topping-availability | Set topping availability |

## Checklist

- [ ] Tạo BrandMapper, BranchMapper, ProductMapper, CategoryMapper, ToppingMapper
- [ ] Tạo BrandService + Controller (CRUD)
- [ ] Tạo BranchService + Controller (CRUD, unique code per brand)
- [ ] Tạo CategoryService + Controller (CRUD, display_order)
- [ ] Tạo ProductService + Controller (CRUD, base_price validation)
- [ ] Tạo ProductVariantService + Controller (CRUD)
- [ ] Tạo ToppingService + Controller (CRUD)
- [ ] Tạo FileStorageService + UploadController
- [ ] Tạo FileStorageConfig / MinIOConfig
- [ ] Implement GET /brands/{brandId}/menu
- [ ] Implement GET /branches/{branchId}/menu (với availability override)
- [ ] Tạo BranchProductAvailability + BranchToppingAvailability repository/service
- [ ] Thêm PATCH endpoints cho availability
- [ ] Test từng CRUD endpoint
- [ ] Test menu API trả về đúng cấu trúc
- [ ] Test upload file
