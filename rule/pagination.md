# Pagination Rule

Use pagination for every API that returns a growing collection from database.

## When To Use

Use pagination when endpoint returns data that can grow by user, order, time, or admin activity.

Examples:

- Admin tables: products, orders, branches, customers, vouchers
- History/log tables: audit logs, callback logs, outbox events, daily stock logs
- Relation lists: orders by customer, products by category, daily stock by branch
- Search/filter/sort endpoints
- Mobile/web list screens and infinite scroll screens

Rule of thumb:

- If response can exceed 20 items, paginate it.
- If endpoint returns `List<T>` from DB, consider `PageResponse<T>` by default.
- If endpoint has search/filter/sort, paginate it.
- If data grows over time, paginate it.

## When Not To Use

Do not use pagination for fixed or small payloads.

Examples:

- Detail endpoint: `GET /resources/{id}`
- Auth/profile endpoints
- Small enum/config lists: status, role type, payment provider, channel
- File upload/download endpoints
- External geocoding search with fixed `limit`

## Standard API Shape

Controller response must use:

```java
BaseResponse<PageResponse<T>>
```

Request should use Spring `Pageable`:

```java
@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
```

Client query format:

```http
GET /api/v1/products?page=0&size=20&sort=createdAt,desc
```

Default values:

- `page`: `0`
- `size`: `20`
- default sort: `createdAt,desc`
- max size: `100`

## Repository Rule

Repository list methods should return `Page<Entity>` and accept `Pageable`.

```java
Page<Branch> findByStatus(String status, Pageable pageable);

Page<Product> findByCategoryId(String categoryId, Pageable pageable);
```

Avoid returning `List<Entity>` for DB list endpoints unless data is fixed and small.

## Service Rule

Service maps entity page content to DTO, then uses `PageResponse.from(page, content)`.

```java
Page<Branch> branches = branchRepository.findAll(pageable);

List<BranchResponse> content = branches.getContent()
        .stream()
        .map(branchMapper::toResponse)
        .toList();

return PageResponse.from(branches, content);
```

Do not expose entity pages directly.

Bad:

```java
return PageResponse.from(branches);
```

Good:

```java
return PageResponse.from(branches, content);
```

## Controller Rule

Use `Pageable` directly instead of manual `page`, `size`, `sort` parsing.

```java
@GetMapping
public ResponseEntity<BaseResponse<PageResponse<BranchResponse>>> getAll(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    PageResponse<BranchResponse> response = branchService.getAll(pageable);
    return ResponseEntity.ok(BaseResponse.success(response, "Branches retrieved successfully"));
}
```

## Search And Filter Rule

Search/filter endpoints still use pagination.

```java
@GetMapping
public ResponseEntity<BaseResponse<PageResponse<ProductResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
    PageResponse<ProductResponse> response = productService.getAll(keyword, status, pageable);
    return ResponseEntity.ok(BaseResponse.success(response, "Products retrieved successfully"));
}
```

## Module Checklist

Before generating any new module, check list endpoints:

- Does endpoint return many rows from DB?
- Can data grow over time?
- Is endpoint used by admin table?
- Does endpoint support search/filter/sort?
- Does response currently return `List<T>`?

If yes to any item, use pagination.

## Project Convention

Use existing class:

```text
src/main/java/com/hoandev/pinedrink/entity/dto/response/PageResponse.java
```

Expected response body shape:

```json
{
  "data": {
    "content": [],
    "page": 0,
    "size": 20,
    "numberOfElements": 0,
    "totalElements": 0,
    "totalPages": 0,
    "first": true,
    "last": true,
    "hasNext": false,
    "hasPrevious": false
  },
  "message": "Success"
}
```
