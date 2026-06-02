# Code ALL PROJECTS

Use this rule for product create/update API design.

## Core Rule

Product `code` is system-generated only.

- Do not accept `code` in `CreateProductRequest`
- Do not accept `code` in `UpdateProductRequest`
- Service generates `code` when creating product
- Product `code` is immutable after creation
- Response may return `code` for display/search

## Service Pattern

```java
String productCode = resolveCreateCode();
product.setCode(productCode);
```

## Avoid

```java
request.getCode()
product.setCode(request.getCode())
```

Compile with `./mvnw -q -DskipTests compile` after changing product flow.
