# Database Schema

Pine Drink uses a no-brand, branch-first schema. There is no `ce_brand`, `ce_brand_domain`, or `brand_id` column.

## Prefixes

| Prefix | Module |
| --- | --- |
| `ce_` | Core branch/config |
| `ia_` | Identity and access |
| `cu_` | Customer and loyalty |
| `pr_` | Product catalog |
| `mn_` | Branch menu availability |
| `ca_` | Cart |
| `od_` | Order and delivery |
| `vc_` | Voucher |
| `py_` | Payment |
| `nt_` | Notification/template |
| `rp_` | Report/export |
| `pf_` | Platform/outbox/idempotency |
| `iv_` | Inventory/recipe/stock |

## Core Branch

### `ce_branch`

Root operational unit.

Key columns: `id`, `code`, `name`, `address`, `phone`, `email`, `latitude`, `longitude`, `timezone`, `supports_pickup`, `supports_delivery`, `average_preparation_minutes`, `status`.

Keys/indexes:
- `UNIQUE(code)`
- `INDEX(status)`
- `INDEX(latitude, longitude)`

### `ce_branch_hours`

Opening hours per branch and weekday.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` cascade
- `UNIQUE(branch_id, day_of_week)`
- `CHECK(day_of_week BETWEEN 1 AND 7)`

### `ce_pickup_time_slot`

Pickup slots per branch.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` cascade
- `UNIQUE(branch_id, slot_code)`
- `INDEX(branch_id, status)`
- `INDEX(branch_id, start_time, end_time)`

### `ce_setting`

Global or branch-specific settings.

Key columns: `scope_type`, `branch_id NULL`, `config_key`, `config_value`, `data_type`, `is_runtime_editable`, `status`.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` cascade
- `UNIQUE(scope_type, branch_id, config_key)`
- `INDEX(branch_id)`, `INDEX(config_key)`, `INDEX(status)`

## Identity And Access

### `ia_account`

User account. No account brand column.

Keys/indexes:
- `UNIQUE(username)`
- `UNIQUE(email)`
- `UNIQUE(phone)`
- `INDEX(status)`

### `ia_scope`

Authorization scope.

Supported scope types:
- `SYSTEM`: all branches
- `BRANCH`: one branch

Key columns: `scope_type`, `branch_id NULL`, `status`.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` cascade
- `UNIQUE(scope_type, branch_id)`
- `INDEX(scope_type, status)`
- `INDEX(branch_id, status)`

### `ia_role`, `ia_permission`, `ia_role_permission`, `ia_account_role_assignment`

RBAC tables. Permissions are stored without `PERM_` prefix and are expanded by backend into Spring authorities.

Assignment keys:
- FK `account_id -> ia_account(id)` cascade
- FK `role_id -> ia_role(id)` restrict
- FK `scope_id -> ia_scope(id)` restrict
- `UNIQUE(account_id, role_id, scope_id)`

### `ia_audit_log`

Audit records. Optional `branch_id` stores branch context when action is branch-scoped.

## Customer And Loyalty

### `cu_customer_profile`

Customer profile linked to account.

### `cu_customer_address`

Customer addresses.

### `cu_loyalty_account`, `cu_loyalty_point_history`

Loyalty state and point history.

## Product Catalog

Catalog is global for Pine Drink.

### `pr_category`

Keys/indexes:
- `UNIQUE(code)`
- `INDEX(status, display_order)`

### `pr_product`

Keys/indexes:
- FK `category_id -> pr_category(id)` restrict
- `UNIQUE(code)`
- `INDEX(category_id, status)`
- `INDEX(status)`
- `INDEX(is_featured, status)`

### `pr_product_variant`

Keys/indexes:
- FK `product_id -> pr_product(id)` cascade
- `UNIQUE(product_id, variant_code)`
- `INDEX(product_id, status, display_order)`

### `pr_topping`

Keys/indexes:
- `UNIQUE(code)`
- `INDEX(status)`

### `pr_product_topping`

Product-topping relation.

Keys/indexes:
- FK product cascade
- FK topping restrict
- `UNIQUE(product_id, topping_id)`

## Menu Availability

### `mn_branch_product_availability`

Branch-specific product sale state and optional sale price.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` cascade
- FK `product_id -> pr_product(id)` cascade
- `UNIQUE(branch_id, product_id)`
- `INDEX(branch_id, is_available)`

### `mn_branch_topping_availability`

Branch-specific topping availability.

Keys/indexes:
- FK branch cascade
- FK topping cascade
- `UNIQUE(branch_id, topping_id)`

## Cart

### `ca_cart`

Cart belongs to branch and either customer or session.

Keys/indexes:
- FK branch restrict
- FK customer cascade
- `INDEX(customer_id, status)`
- `INDEX(session_id, status)`
- `INDEX(branch_id)`

### `ca_cart_item`, `ca_cart_item_topping`

Cart line items and topping selections.

## Order And Delivery

### `od_order`

Order belongs to one branch.

Statuses:
- `PENDING`
- `CONFIRMED`
- `PREPARING`
- `READY`
- `DELIVERING`
- `COMPLETED`
- `CANCELLED`
- `REJECTED`

Payment methods:
- `CASH`
- `COD`
- `VNPAY`
- `MOMO`
- `BANK_TRANSFER`

Important timestamps: `confirmed_at`, `prepared_at`, `ready_at`, `delivering_at`, `delivered_at`, `completed_at`, `cancelled_at`, `rejected_at`.

Keys/indexes:
- FK `branch_id -> ce_branch(id)` restrict
- FK `customer_id -> cu_customer_profile(id)` set null
- `UNIQUE(order_code)`
- `INDEX(customer_id, created_at)`
- `INDEX(branch_id, created_at)`
- `INDEX(branch_id, status, created_at)`
- `INDEX(branch_id, order_type, status, created_at)`
- `INDEX(branch_id, payment_method, created_at)`
- `INDEX(branch_id, payment_status, created_at)`

### `od_order_delivery`

Delivery detail and shipper assignment for delivery orders.

Statuses:
- `PENDING`
- `ASSIGNED`
- `PICKED_UP`
- `DELIVERING`
- `DELIVERED`
- `FAILED`
- `CANCELLED`

Keys/indexes:
- FK `order_id -> od_order(id)` cascade
- FK `shipper_id -> ia_account(id)` set null
- `UNIQUE(order_id)`
- `INDEX(shipper_id, status, created_at)`
- `INDEX(status, created_at)`

### `od_order_item`, `od_order_item_topping`, `od_order_status_history`

Immutable order line snapshot and status history.

## Voucher And Payment

### `vc_voucher`

Global voucher definition.

Keys/indexes:
- `UNIQUE(code)`
- `INDEX(status, start_at, end_at)`

### `vc_voucher_branch`

Optional branch limitation for vouchers.

Keys/indexes:
- FK voucher cascade
- FK branch cascade
- `UNIQUE(voucher_id, branch_id)`

### `vc_voucher_usage`

Voucher usage per order/customer.

### `py_payment_intent`, `py_transaction`, `py_callback_log`, `py_refund`

Payment provider integration and refund records.

## Notification, Report, Platform

### `nt_notification`

Notification for account and optional branch context.

### `nt_template`

Global notification template.

Keys/indexes:
- `UNIQUE(template_code, channel)`
- `INDEX(status)`

### `rp_export_request`

Report export request with optional branch filter.

### `pf_outbox_event`

Outbox events with optional branch routing context.

### `pf_idempotency_key`

Request idempotency storage.

## Inventory

### `iv_ingredient`

Global ingredient master data.

Keys/indexes:
- `UNIQUE(code)`
- `INDEX(status)`

### `iv_recipe`, `iv_recipe_item`

Recipe definition per product/variant.

### `iv_stock`

Branch stock per ingredient.

Keys/indexes:
- FK branch cascade
- FK ingredient restrict
- `UNIQUE(branch_id, ingredient_id)`
- `INDEX(branch_id)`

### `iv_stock_movement`

Stock movement history per branch/ingredient/order.

Keys/indexes:
- FK branch restrict
- FK ingredient restrict
- FK order set null
- `INDEX(branch_id, ingredient_id, created_at)`
- `INDEX(order_id)`
- `INDEX(movement_type, created_at)`

## Relationship Summary

```text
ce_branch ── ce_branch_hours
ce_branch ── ce_pickup_time_slot
ce_branch ── ca_cart ── ca_cart_item ── ca_cart_item_topping
ce_branch ── od_order ── od_order_delivery
ce_branch ── od_order ── od_order_item ── od_order_item_topping
ce_branch ── iv_stock ── iv_ingredient
ce_branch ── mn_branch_product_availability ── pr_product
ce_branch ── mn_branch_topping_availability ── pr_topping

pr_category ── pr_product ── pr_product_variant
pr_product ── pr_product_topping ── pr_topping
pr_product ── iv_recipe ── iv_recipe_item ── iv_ingredient

ia_account ── ia_account_role_assignment ── ia_scope ── ce_branch
ia_role ── ia_role_permission ── ia_permission
```
