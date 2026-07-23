# Plan 11 - Account Management

Account management is no-brand and branch-first.

## Features

- Search accounts by keyword, status, role code.
- Create internal accounts.
- Update account profile fields.
- Change account status.
- Reset password.
- View role assignments.
- Assign/revoke roles with `SYSTEM` or `BRANCH` scope.

## Search Filters

Supported filters:

- `keyword`
- `status`
- `roleCode`
- `page`, `size`, `sort`

No `brandId` filter.

## Role Assignment

Scope types:

- `SYSTEM`: full app access
- `BRANCH`: selected branch

Example:

```json
{
  "roleCode": "DELIVERY",
  "scopeType": "BRANCH",
  "branchId": "...",
  "expiresAt": null
}
```

## Response Rule

Account responses must not include `brandId`.

Role assignment responses may include:

- `scopeId`
- `scopeType`
- `scopeBranchId`
- `status`
- `assignedAt`
- `expiresAt`

## Authorization

- Account management normally requires `SYSTEM` scope.
- Branch-scoped users cannot manage system accounts.
- Cache invalidation required after assign/revoke.
