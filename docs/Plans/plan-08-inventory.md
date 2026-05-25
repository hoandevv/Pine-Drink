# Plan 8 — Inventory Management (Optional)

## Mục tiêu
Quản lý nguyên liệu (ingredients), recipe cho sản phẩm, tồn kho theo branch, và tự động trừ/reserve stock khi xử lý đơn hàng.

## Files cần tạo

| File | Mô tả |
|------|-------|
| `service/IngredientService.java` | Interface IngredientService |
| `service/impl/IngredientServiceImpl.java` | Implementation |
| `service/RecipeService.java` | Interface RecipeService |
| `service/impl/RecipeServiceImpl.java` | Implementation |
| `service/StockService.java` | Interface StockService |
| `service/impl/StockServiceImpl.java` | Implementation |
| `controller/IngredientController.java` | REST controller cho ingredient CRUD |
| `controller/RecipeController.java` | REST controller cho recipe CRUD |
| `controller/StockController.java` | REST controller cho stock management |

## Chi tiết implementation

### 1. Ingredient CRUD

```java
@Entity
@Table(name = "in_ingredient")
public class Ingredient {
    @Id
    private UUID id;

    private String code;           // mã nguyên liệu
    private String name;           // tên
    private String unit;           // kg, lít, ml, gram, cái
    private Double minStockQuantity; // ngưỡng cảnh báo tồn tối thiểu
    private UUID brandId;          // thuộc brand nào
}
```

### 2. Recipe Management

```java
@Entity
@Table(name = "in_recipe")
public class Recipe {
    @Id
    private UUID id;
    private UUID productId; // hoặc variantId
    private String name;
    private String description;
    private Boolean isActive;
}

@Entity
@Table(name = "in_recipe_item")
public class RecipeItem {
    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "recipe_id")
    private Recipe recipe;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    private Double quantity; // số lượng cần
}
```

### 3. Stock Management

```java
@Entity
@Table(name = "in_stock")
public class Stock {
    @Id
    private UUID id;

    private UUID branchId;
    private UUID ingredientId;

    private Double quantityOnHand;   // tồn hiện tại
    private Double reservedQuantity; // đã reserved cho đơn chưa completed

    @Version
    private Long version; // optimistic locking
}

@Entity
@Table(name = "in_stock_movement")
public class StockMovement {
    @Id
    private UUID id;

    private UUID stockId;
    private UUID branchId;
    private UUID ingredientId;
    private UUID orderId; // nếu liên quan đến đơn
    private String type;  // IMPORT, EXPORT, ADJUSTMENT, RESERVE, RELEASE
    private Double quantityBefore;
    private Double quantityChange;
    private Double quantityAfter;
    private String reference;
    private UUID createdBy;
}
```

### 4. StockService — Reserve & Deduct

```java
@Service
@RequiredArgsConstructor
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;
    private final StockMovementRepository movementRepository;
    private final RecipeRepository recipeRepository;
    private final RecipeItemRepository recipeItemRepository;

    @Override
    @Transactional
    public void reserveStock(Order order) {
        List<RecipeItem> items = getRecipeItemsForOrder(order);

        for (RecipeItem item : items) {
            Stock stock = stockRepository
                .findByBranchIdAndIngredientId(order.getBranchId(), item.getIngredient().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Stock not found"));

            double needed = item.getQuantity() * getOrderItemQuantity(order, item);

            if (stock.getQuantityOnHand() - stock.getReservedQuantity() < needed) {
                throw new InsufficientStockException(
                    "Insufficient stock for ingredient: " + item.getIngredient().getName());
            }

            stock.setReservedQuantity(stock.getReservedQuantity() + needed);
            stockRepository.save(stock);

            recordMovement(stock, "RESERVE", needed, order.getId());
        }
    }

    @Override
    @Transactional
    public void deductStock(Order order) {
        List<RecipeItem> items = getRecipeItemsForOrder(order);

        for (RecipeItem item : items) {
            Stock stock = stockRepository
                .findByBranchIdAndIngredientId(order.getBranchId(), item.getIngredient().getId())
                .orElseThrow();

            double needed = item.getQuantity() * getOrderItemQuantity(order, item);

            stock.setQuantityOnHand(stock.getQuantityOnHand() - needed);
            stock.setReservedQuantity(stock.getReservedQuantity() - needed);
            stockRepository.save(stock);

            recordMovement(stock, "EXPORT", -needed, order.getId());
        }
    }

    @Override
    @Transactional
    public void releaseReservedStock(Order order) {
        // Khi order bị cancel, release reserved quantity
        List<RecipeItem> items = getRecipeItemsForOrder(order);

        for (RecipeItem item : items) {
            Stock stock = stockRepository
                .findByBranchIdAndIngredientId(order.getBranchId(), item.getIngredient().getId())
                .orElseThrow();

            double needed = item.getQuantity() * getOrderItemQuantity(order, item);
            stock.setReservedQuantity(stock.getReservedQuantity() - needed);
            stockRepository.save(stock);

            recordMovement(stock, "RELEASE", needed, order.getId());
        }
    }

    private void recordMovement(Stock stock, String type, Double change, UUID orderId) {
        StockMovement movement = StockMovement.builder()
                .stockId(stock.getId())
                .branchId(stock.getBranchId())
                .ingredientId(stock.getIngredientId())
                .orderId(orderId)
                .type(type)
                .quantityBefore(stock.getQuantityOnHand())
                .quantityChange(change)
                .quantityAfter(stock.getQuantityOnHand() + change)
                .build();
        movementRepository.save(movement);
    }
}
```

### 5. Auto low stock alert

```java
@Scheduled(cron = "0 0 */6 * * *") // mỗi 6h
public void checkLowStock() {
    List<Stock> lowStocks = stockRepository.findLowStock();
    for (Stock stock : lowStocks) {
        notificationService.sendLowStockAlert(stock);
    }
}
```

## API Endpoints

| Method | Endpoint | Mô tả |
|--------|----------|-------|
| GET | `/ingredients?brandId=` | Danh sách ingredients |
| POST | `/ingredients` | Tạo ingredient mới |
| PUT | `/ingredients/{id}` | Cập nhật ingredient |
| DELETE | `/ingredients/{id}` | Xóa ingredient |
| GET | `/recipes?productId=` | Danh sách recipes theo product |
| POST | `/recipes` | Tạo recipe |
| PUT | `/recipes/{id}` | Cập nhật recipe |
| DELETE | `/recipes/{id}` | Xóa recipe |
| GET | `/stocks?branchId=&ingredientId=` | Xem tồn kho theo branch |
| PATCH | `/stocks/adjust` | Điều chỉnh tồn kho (import/adjust) |
| GET | `/stocks/movements?branchId=&fromDate=&toDate=` | Lịch sử biến động tồn |

## Stock Flow theo Order

```
Order CONFIRMED → reserveStock() → reservedQuantity += needed
Order COMPLETED → deductStock()  → quantityOnHand -= needed, reservedQuantity -= needed
Order CANCELLED → releaseReservedStock() → reservedQuantity -= needed
```

## Checklist

- [ ] Tạo Ingredient entity + repository
- [ ] Tạo Recipe + RecipeItem entity + repositories
- [ ] Tạo Stock entity (branchId, ingredientId, quantityOnHand, reservedQuantity)
- [ ] Tạo StockMovement entity (ghi log mọi biến động)
- [ ] Tạo IngredientService + controller (CRUD)
- [ ] Tạo RecipeService + controller (CRUD)
- [ ] Tạo StockService + controller
- [ ] Implement reserveStock khi order CONFIRMED
- [ ] Implement deductStock khi order COMPLETED
- [ ] Implement releaseReservedStock khi order CANCELLED
- [ ] Optimistic locking cho Stock (version field)
- [ ] Low stock alert scheduling
- [ ] Ghi StockMovement cho mọi biến động
