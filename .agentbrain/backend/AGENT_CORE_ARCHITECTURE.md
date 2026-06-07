# CORE ARCHITECTURE & CODING STANDARDS - PCCMS PROJECT

## 1. Tech Stack

- **Backend:** Java Spring Boot, Spring Data JPA, Hibernate.
- **Frontend:** React, Vite.
- **Database:** PostgreSQL.

## 2. Global Project Structure

Agent must strictly follow the common package structure defined in `com.pccms.common`:

- Use `com.pccms.common.response.ApiResponse<T>` for all REST controller successful returns.
- Use `com.pccms.common.response.PageResponse<T>` for pagination.
- Throw `com.pccms.common.exception.BusinessException` with appropriate `ErrorCode` for business rule violations.
- Entities must extend `com.pccms.common.domain.AuditableEntity` (for `created_at`, `updated_at`, `created_by`, `updated_by`).

## 3. Database & Entity Rules

- **No Hard Deletes:** Use `is_active` boolean or status codes to perform soft deletes on master tables (users, pets, medicines).
- Use `UUID` for all primary keys in Java (mapped to `gen_random_uuid()` in Postgres).
