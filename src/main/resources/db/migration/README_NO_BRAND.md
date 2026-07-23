# Pine Drink migrations without Brand

This folder contains V1 -> V13 rewritten for a simpler Pine Drink domain: no-brand, branch-first.

Removed:
- `ce_brand`
- `ce_brand_domain`
- every `brand_id` column
- every foreign key/index/unique key that depended on brand

Kept:
- `ce_branch`
- branch-scoped availability, order, inventory, report, notification structures
- RBAC scope via `ia_scope.scope_type + branch_id`

Use this set when you can reset the dev database and run Flyway from scratch.
Do not apply this folder on top of an existing database that already ran the old brand-based migrations.
