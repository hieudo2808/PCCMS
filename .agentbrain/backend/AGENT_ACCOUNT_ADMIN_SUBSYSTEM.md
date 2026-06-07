# SUBSYSTEM: ACCOUNT ADMINISTRATION (Account & Pet Profiles)

## 1. Domain Models (Entities)

### `User` (Mapped to `users` table)

### `Pet` (Mapped to `pets` table)

Look up at `database\pccms_database_schema.sql`

## 2. Business Logic Guardrails (Agent must implement these)

1. **Soft Delete for Pets:** When an endpoint requests to delete a pet, perform an UPDATE setting `is_active = false`. Do not execute `repository.delete()`.
2. **Owner Verification:** Before adding a pet, verify `owner_id` exists and belongs to a user with role 'CUSTOMER'.
3. **Data Validation:** `weight_kg` must be strictly > 0.
4. **DTO Mapping:** Use MapStruct or manual mapping to prevent returning sensitive `User` fields (like `password_hash`) in Pet profile responses.
