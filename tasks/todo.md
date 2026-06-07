# PCCMS Frontend — Execution Checklist (TDD)

> Dựa trên [plan.md](./plan.md)

## Phase A: Foundation (TDD Setup)

- [x] **A1** — `pnpm add` các core + testing packages
- [x] **A2** — TDD & Testing Setup (Vitest, RTL, MSW, setupTests.ts)
- [x] **A3** — `src/types/*.ts` định nghĩa toàn bộ backend DTOs
- [x] **A4** — (TDD) `axiosClient.ts` + `errorHandlers.ts`
- [x] **A5** — `QueryClientProvider` + `queryKeys.ts`
- [x] **A6** — (TDD) Shared UI components (Button, Badge, Skeletons, States)
- [x] **A7** — (TDD) `routes.ts` + `useDebounce.ts`

### ✅ Checkpoint A
- [x] `pnpm build` → SUCCESS
- [x] `pnpm test` → 100% tests pass (10+ tests)
- [x] App khởi động không lỗi

---

## Phase B: Auth (Replace mockAuth)

- [x] **B1** — `authApi.ts` + `authSchema.ts` (loginSchema, registerSchema)
- [x] **B2** — (TDD) `useAuth` hook + `AuthContext` (AuthProvider, JWT decode, logout)
- [x] **B3** — (TDD) `LoginPage` wiring + `AuthGuard` real JWT check
- [x] **B4** — (TDD) `RegisterPage` wiring + Header logout + Avatar

### ✅ Checkpoint B
- [x] `mockAuth` đã xóa hoàn toàn
- [x] TDD Tests cho Auth flow passed
- [x] Login/Register/Logout hoạt động với backend thực

---

## Phase C: User Profile

- [x] **C1** — (TDD) `userApi.ts` + `ProfilePage` wiring (3 states, form edit, change password)

---

## Phase D: Pet Management (Owner)

- [x] **D1** — (TDD) `petApi.ts` + `PetProfilesPage` + `PetForm` (CRUD, alerts)

### ✅ Checkpoint D
- [x] Owner có thể quản lý pet, thấy health alerts

---

## Phase E: Medical Record & Prescription (Doctor)

- [x] **E1** — `medicalRecordApi.ts` + `prescriptionApi.ts`
- [x] **E2** — (TDD) `VitalSignsForm` (Zod, realtime SpO2 validation)
- [x] **E3** — (TDD) `PrescriptionTable` (dynamic rows, autocomplete, totalQuantity)
- [x] **E4** — (TDD) `MedicalRecordPage` + UI Lock (FINALIZED record disabled tất cả)

### ✅ Checkpoint E
- [x] UI Lock hoạt động 100% khi status là FINALIZED
- [x] Doctor có thể thêm chỉ số sinh tồn và kê đơn thuốc

---

## Phase F: Medicine Catalog (Admin)

- [x] **F1** — (TDD) `medicineApi.ts` + `CatalogPage` CRUD + Nhập kho

### ✅ Checkpoint F
- [x] Validate luồng Data Fetching `Medicine` và form validation.

---

## Phase G: Account Management (Admin)

- [x] **G1** — (TDD) `userAdminApi.ts` + `AccountsPage` CRUD + Khóa tài khoản

### ✅ Checkpoint Final
- [x] `pnpm build` SUCCESS
- [x] `pnpm test` pass toàn bộ codebase
- [x] Tất cả functions wiring hoàn chỉnh với backend Spring Boot.

---

## Backend Phase 1: Database Schema & Identity Refactor (STRICT TDD)
- [x] **B-P1.0** — Thiết kế Test (Blackbox BVA, EP) & tạo file CSV test data cho Identity & User.
- [x] **B-P1.1** — Sửa `pccms_database_schema.sql` (Thêm `role_id` vào `users`, xóa `user_roles`).
- [x] **B-P1.2** — Viết Integration Test cho `Users` mapping. Refactor `Users.java` (đổi `userId` -> `id`, thêm `phone`, xóa trường thừa).
- [x] **B-P1.3** — Viết Integration Test cho `Roles`, `Permission`. Refactor `Roles.java` & `Permission.java`. Cập nhật bảng trung gian `role_permissions`.
- [x] **B-P1.4** — Viết Integration Test cho `RefreshToken`. Refactor `RefreshToken.java`.
- [x] **B-P1.5** — Viết Integration Test cho `Notification`. Đập đi xây lại `EmailNotification` -> `Notification` (Skipped - Unimplemented service).
- [x] **B-P1.6** — Update DTOs, Mappers. Viết Unit Test 100% Branch Coverage cho các Service bị ảnh hưởng (Login/Register).

---

## Frontend Phase 2: Sync & Alignment (STRICT TDD)
- [x] **F-P2.1** — Cập nhật `src/types/user.ts`, `auth.ts`, `authSchema.ts` (Sửa UserRole thành OWNER/STAFF, update UserResponse schema).
- [x] **F-P2.2** — (TDD) Update MSW Mock handlers (`handlers.ts`), fix `AuthContext.tsx` và pass toàn bộ unit tests auth.
- [x] **F-P2.3** — (TDD) Refactor `ProfilePage.tsx` (xóa Avatar upload, đổi `userId` -> `id`), pass unit test `ProfilePage.test.tsx`.
- [x] **F-P2.4** — Refactor `DashboardLayout.tsx` & Guard Routes, chạy `pnpm test` pass toàn bộ frontend codebase.
