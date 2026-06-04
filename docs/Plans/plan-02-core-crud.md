# Plan 02 - Core CRUD

Pine Drink uses a no-brand, branch-first domain model.

## Scope

Implemented/core modules:

- Branch
- Category
- Product
- Product variant
- Topping
- Product topping
- Branch product availability
- Branch topping availability
- File upload for product/category/topping images

There is no Brand CRUD.

## API Shape

Branch APIs:

```http
GET /api/v1/branches?page=0&size=20
GET /api/v1/branches/active?page=0&size=20
GET /api/v1/branches/{id}
POST /api/v1/branches
PUT /api/v1/branches/{id}
PATCH /api/v1/branches/{id}/status
DELETE /api/v1/branches/{id}
```

Product APIs:

```http
GET /api/v1/products?page=0&size=20
GET /api/v1/products?categoryId={categoryId}&page=0&size=20
GET /api/v1/products/{id}
POST /api/v1/products
PUT /api/v1/products/{id}
PATCH /api/v1/products/{id}/status
DELETE /api/v1/products/{id}
```

## Data Rule

- Category, product, topping, voucher, ingredient, template are global.
- Branch, cart, order, delivery, stock, availability are branch-scoped.
- No request/response should expose `brandId`.
- No repository should use `findByBrand...`.

## Authorization

- Controller checks `PERM_*` permission.
- Service checks branch scope only when target data is branch-scoped.
- Global catalog mutation requires `SYSTEM` scope.

## Validation

- Branch code unique globally.
- Category code unique globally.
- Product code unique globally.
- Topping code unique globally.
- Product category must exist.
- Price fields must be non-negative.

## Compile

Run:

```bash
./mvnw -q -DskipTests compile
```
