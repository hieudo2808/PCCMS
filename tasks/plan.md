# PCCMS Frontend — Implementation Plan
## Kết nối React/Vite với Spring Boot Backend (TDD Approach)

> **Spec:** [SPEC.md](../SPEC.md) | **Last updated:** 2026-06-03

---

## Overview

Wiring toàn bộ giao diện React (đã có đầy đủ UI pages) với backend Spring Boot 4 thực tế theo phương pháp **Test-Driven Development (TDD)**. Thay thế `mockAuth` + hardcoded data bằng real API calls sử dụng React Query, React Hook Form, và Zod. 

Tổng cộng **7 Phases** (Foundation → Auth → Profile → Pets → Medical → Medicines → Accounts). 
**Bắt buộc:** Mọi component/tính năng phải được viết test trước (sử dụng Vitest, React Testing Library và MSW) trước khi viết mã triển khai thực tế.

---

## Architecture Decisions

- **TDD & Testing:** Sử dụng Vitest, React Testing Library (RTL). Giả lập API bằng MSW (Mock Service Worker). Tuyệt đối không gọi API thật trong Unit Test.
- **Server State:** React Query (`@tanstack/react-query`) — không dùng `useEffect + setState` cho data fetching.
- **Forms:** React Hook Form + Zod resolver — không dùng `onChange` + `useState` thủ công.
- **Notifications:** `react-hot-toast` — tập trung xử lý lỗi backend tại interceptor.
- **FSD Structure:** `src/shared/` cho code dùng chung, `src/features/` cho domain logic.
- **Auth:** JWT decode từ `localStorage`, AuthContext expose `{ user, role, isAuthenticated, logout }`.
- **UI Lock:** `record.recordStatus === 'FINALIZED'` → tất cả inputs `disabled`, ẩn action buttons.

---

## Dependency Graph

```
[Task A1] Packages (App + Testing)
    │
    ├── [Task A2] TDD Setup (Vitest, RTL, MSW config)
    │       │
    │       ├── [Task A3] Types định nghĩa (ApiResponse, User, Pet, ...)
    │       │       │
    │       │       ├── [Task A4] axiosClient refactor + Error Handling
    │       │       │       │
    │       │       │       └── [Task A5] React Query Provider + queryKeys
    │       │       │               │
    │       │       │               └── [Task A6] Shared UI components (TDD)
    │       │       │                       │
    │       │       │                       └── [Task A7] routes.ts constants
    │
    ├── [CHECKPOINT A] ← Foundation hoàn chỉnh (100% tests pass)
    │
    ├── [Task B1] authApi.ts + authSchema
    │       │
    │       └── [Task B2] useAuth hook + AuthContext (TDD)
    │               │
    │               └── [Task B3] LoginPage + AuthGuard (TDD)
    │                       │
    │                       └── [Task B4] RegisterPage + Header logout (TDD)
    │
    ├── [CHECKPOINT B] ← Auth flow hoàn chỉnh
    │
    ├── [Task C1] ProfilePage (TDD)
    │
    ├── [Task D1] PetProfilesPage (TDD)
    │
    ├── [CHECKPOINT D] ← Owner role hoàn chỉnh
    │
    ├── [Task E1] medicalRecordApi & prescriptionApi
    │       │
    │       ├── [Task E2] VitalSignsForm (TDD)
    │       │
    │       └── [Task E3] PrescriptionTable (TDD)
    │               │
    │               └── [Task E4] MedicalRecordPage + UI Lock (TDD)
    │
    ├── [CHECKPOINT E] ← Doctor role hoàn chỉnh
    │
    ├── [Task F1] CatalogPage (Medicines CRUD) (TDD)
    │
    └── [Task G1] AccountsPage (Users CRUD) (TDD)
```

---

## Phase A: Foundation

> **Prerequisite cho tất cả các phase sau. PHẢI hoàn thành trước khi bắt đầu bất kỳ task nào khác.**

---

### Task A1: Cài đặt packages mới (Core & Testing)

**Description:** Cài thêm packages cần thiết cho production và testing.

**Acceptance criteria:**
- [x] Core: `pnpm add @tanstack/react-query react-hook-form zod @hookform/resolvers react-hot-toast jwt-decode`
- [x] Testing: `pnpm add -D vitest jsdom @testing-library/react @testing-library/jest-dom @testing-library/user-event msw`
- [x] `pnpm build` không có lỗi TypeScript.

**Verification:**
- [x] `cat package.json` hiển thị đầy đủ packages.

**Estimated scope:** XS

---

### Task A2: TDD & Testing Setup

**Description:** Cấu hình Vitest, RTL, và MSW để sẵn sàng viết test cho các component.

**Acceptance criteria:**
- [x] Cấu hình `vite.config.ts` để hỗ trợ vitest (`test` block với environment `jsdom`).
- [x] Tạo file `src/setupTests.ts` import `@testing-library/jest-dom`.
- [x] Thiết lập MSW: tạo `src/mocks/handlers.ts`, `src/mocks/browser.ts`, `src/mocks/server.ts`.
- [x] Viết một test mẫu đơn giản `App.test.tsx` chạy thành công.

**Verification:**
- [x] `pnpm test` chạy thành công test mẫu.

**Estimated scope:** S

---

### Task A3: Định nghĩa TypeScript Types

**Description:** Tạo toàn bộ TypeScript types mapping với backend DTOs (Dựa vào controller đã xem).

**Acceptance criteria:**
- [x] `src/types/api.ts` — `ApiResponse<T>`, `PageResponse<T>`
- [x] `src/types/user.ts` — `UserResponse`, `UserRole`, `UserStatus`, v.v.
- [x] `src/types/pet.ts` — `PetResponse`, `PetRequest`, `HealthAlertResponse`
- [x] `src/types/medicalRecord.ts` — `MedicalRecordResponse`, `RecordStatus`, v.v.
- [x] `src/types/medicine.ts` — `MedicineResponse`, v.v.
- [x] `src/types/auth.ts` — `LoginRequest`, `AuthResponse` (gồm token, refreshToken, user)
- [x] Tất cả types export từ `src/types/index.ts`

**Verification:**
- [x] `pnpm build` → BUILD SUCCESS

**Estimated scope:** S

---

### Task A4: Refactor axiosClient + Error Handling (TDD)

**Description:** Refactor `axiosClient.ts`. Phải viết test (dùng MSW mock error response) trước khi implement.

**Acceptance criteria:**
- [x] **TDD:** Viết test kiểm tra interceptor tự động unwrap `response.data.data`.
- [x] **TDD:** Viết test kiểm tra 401 response sẽ gọi clear token và redirect.
- [x] **TDD:** Viết test kiểm tra 403 response gọi hiển thị toast.
- [x] Implement `axiosClient.ts` để pass các tests trên.
- [x] Implement `errorHandlers.ts` để parse lỗi từ `ApiResponse`.

**Verification:**
- [x] `pnpm test` cho axiosClient pass 100%.

**Estimated scope:** S

---

### Task A5: React Query Provider + Query Key Factory

**Description:** Setup `QueryClient` với config chuẩn, wrap app trong `QueryClientProvider` và `Toaster`.

**Acceptance criteria:**
- [x] `src/app/providers.tsx` wrap `QueryClientProvider` + `<Toaster position="top-right" />`.
- [x] `src/shared/api/queryKeys.ts` export factory.

**Verification:**
- [x] App khởi động không lỗi.

**Estimated scope:** XS

---

### Task A6: Shared UI Components (TDD)

**Description:** Tạo các shared UI primitives. Bắt buộc test trước khi code.

**Acceptance criteria:**
- [x] **TDD:** Viết test cho `Button` (click event, disabled state, loading spinner).
- [x] **TDD:** Viết test cho `EmptyState`, `ErrorState`, `ConfirmDialog`.
- [x] Implement components bằng Tailwind CSS pass tests.
- [x] Xuất ra từ `src/shared/components/index.ts`.

**Verification:**
- [x] `pnpm test` pass.

**Estimated scope:** M

---

### Task A7: Route Constants + useDebounce Hook (TDD)

**Description:** Tạo `routes.ts` và custom hook.

**Acceptance criteria:**
- [x] **TDD:** Viết test cho `useDebounce` hook (kiểm tra timeout update).
- [x] Implement `useDebounce`.
- [x] Tạo `src/constants/routes.ts` và thay thế hardcode strings trong router.

**Verification:**
- [x] Tests pass. `pnpm build` SUCCESS.

---

### ✅ CHECKPOINT A: Foundation Complete

```
- [x] pnpm build → BUILD SUCCESS
- [x] pnpm test → 100% tests pass (Axios, Components, Hooks)
- [x] MSW và Vitest đã sẵn sàng
```

---

## Phase B: Auth (Replace mockAuth)

---

### Task B1: authApi.ts + authSchema

**Description:** API calls và schemas cho Authentication.

**Acceptance criteria:**
- [x] `authApi.ts` endpoints: `/auth/login`, `/auth/register`, `/auth/logout`.
- [x] `authSchema.ts`: Zod schemas cho form.

**Estimated scope:** XS

---

### Task B2: useAuth Hook + AuthContext (TDD)

**Description:** Hook quản lý Auth state bằng JWT.

**Acceptance criteria:**
- [x] **TDD:** Test `useAuth` trả về user/role khi có JWT hợp lệ trong localStorage.
- [x] **TDD:** Test `logout` clear localStorage và state.
- [x] Implement `AuthContext` + `jwt-decode` để pass tests.

**Estimated scope:** M

---

### Task B3: LoginPage + AuthGuard (TDD)

**Description:** Thay thế mockAuth, xử lý login.

**Acceptance criteria:**
- [x] **TDD:** Viết test render LoginPage.
- [x] **TDD:** Viết test điền form lỗi (validation).
- [x] **TDD:** Viết test submit form thành công (mock MSW `/auth/login`), redirect đúng trang.
- [x] Implement `LoginPage` bằng React Hook Form + Zod.
- [x] Implement `AuthGuard` bảo vệ route dựa vào `useAuth()`.

**Estimated scope:** M

---

### Task B4: RegisterPage + Header Logout (TDD)

**Description:** Xử lý đăng ký và logout ở Header.

**Acceptance criteria:**
- [x] **TDD:** Test validation Register form, submit thành công gọi `/auth/register`.
- [x] **TDD:** Test Header hiển thị đúng tên/avatar, bấm nút Logout gọi hàm logout.
- [x] Implement `RegisterPage` và update `DashboardLayout`.

**Estimated scope:** M

---

### ✅ CHECKPOINT B: Auth Complete

```
- [x] mockAuth bị xóa hoàn toàn.
- [x] Tất cả TDD tests pass (Auth flow).
```

---

## Phase C: User Profile

---

### Task C1: ProfilePage (TDD)

**Description:** Hiển thị và cập nhật hồ sơ cá nhân (`/users/me`).

**Acceptance criteria:**
- [x] **TDD:** Render skeleton khi loading, thông tin khi fetch thành công (`MSW` mock `/users/me`).
- [x] **TDD:** Test form update profile validation.
- [x] Implement `userApi.ts` và `ProfilePage`.

**Estimated scope:** M

---

## Phase D: Pet Management (Owner)

---

### Task D1: PetProfilesPage & PetForm (TDD)

**Description:** Quản lý thú cưng của Customer.

**Acceptance criteria:**
- [x] API: POST/GET/PUT `/api/v1/pets` (và `/api/v1/pets/{id}`).
- [x] **TDD:** Render list/chi tiết pet.
- [x] **TDD:** Modal PetForm validation (tên bắt buộc).
- [x] **TDD:** Hiển thị `HealthAlertResponse` dưới dạng warning badges.
- [x] Implement components pass tests.

**Estimated scope:** L

---

## Phase E: Medical Record & Prescription (Doctor)

---

### Task E1: medicalRecordApi & prescriptionApi

**Description:** Endpoints cho VETERINARIAN.

**Acceptance criteria:**
- [x] `/api/v1/medical-records/{id}` (PUT update, PATCH finalize).
- [x] `/api/v1/medical-records/{id}/prescriptions` (POST).

**Estimated scope:** XS

---

### Task E2: VitalSignsForm (TDD)

**Description:** Form chỉ số sinh tồn có realtime validation.

**Acceptance criteria:**
- [x] **TDD:** Form validate SpO2 > 100 hiển thị lỗi ngay lập tức.
- [x] **TDD:** Form input bị disabled nếu truyền `disabled={true}` prop.
- [x] Implement React Hook Form + Zod cho VitalSigns.

**Estimated scope:** M

---

### Task E3: PrescriptionTable (TDD)

**Description:** Bảng kê đơn thuốc động.

**Acceptance criteria:**
- [x] **TDD:** Thêm hàng, xóa hàng.
- [x] **TDD:** `totalQuantity` tự tính (`dosage * frequency * durationDays`).
- [x] **TDD:** Autocomplete input debounce API call.
- [x] Implement bằng `useFieldArray`.

**Estimated scope:** L

---

### Task E4: MedicalRecordPage + UI Lock (TDD)

**Description:** Chức năng bệnh án và khóa UI khi FINALIZED. ĐÂY LÀ CHỨC NĂNG CỰC KỲ QUAN TRỌNG.

**Acceptance criteria:**
- [x] **TDD:** Nếu mock trả về `recordStatus === 'FINALIZED'`, Assert `VitalSignsForm` và `PrescriptionTable` đều nhận prop disabled. Nút Lưu/Chốt không được render. Badge "Đã chốt" được hiển thị.
- [x] **TDD:** Nút "Chốt bệnh án" gọi PATCH `/api/v1/medical-records/{id}/finalize`.
- [x] Implement integration.

**Estimated scope:** M

---

## Phase F: Medicine Catalog (Admin)

---

### Task F1: CatalogPage & Medicines CRUD (TDD)

**Description:** Quản lý danh mục thuốc và kho.

**Acceptance criteria:**
- [x] API: GET/POST/PUT/DELETE `/api/v1/medicines`, PATCH `/api/v1/medicines/{id}/stock`.
- [x] **TDD:** DataTable hiển thị thuốc, cảnh báo kho < 10.
- [x] **TDD:** Test Modal nhập kho, validate số lượng > 0.
- [x] Implement `CatalogPage`.

**Estimated scope:** L

---

## Phase G: Account Management (Admin)

---

### Task G1: AccountsPage & Users CRUD (TDD)

**Description:** Quản trị viên quản lý user (Khóa/Vô hiệu hóa).

**Acceptance criteria:**
- [x] API: GET/POST/PUT/DELETE `/users`, PATCH `/users/{id}/lock` & `disable`.
- [x] **TDD:** Filter theo Role hoạt động.
- [x] **TDD:** Render đúng màu Badge tùy status (ACTIVE=green, LOCKED=red).
- [x] Implement `AccountsPage`.

**Estimated scope:** L

---

## Open Questions (Đã được giải quyết)

- **API Paths:** Sử dụng quy ước `/auth/*`, `/users/*`, `/api/v1/pets/*` như trong backend controllers. Nếu base Axios URL trỏ đến `localhost:8080/`, ta sẽ gắn thẳng endpoint theo đúng controller (VD: `/api/v1/pets`).
- **Auth Response:** Đã có `token`, `refreshToken` và `UserResponse` trong `AuthResponse.java`.
- **Testing:** Chuyển hoàn toàn sang quy trình TDD. Test phải được tạo bằng Vitest + RTL, mock bằng MSW trước khi viết logic React.

---

## 🚀 NEW: BACKEND DATABASE ALIGNMENT PLAN (STRICT TDD) 🚀

**Goal:** Đồng bộ hóa mã nguồn Backend và cơ sở dữ liệu dựa trên **các service đã được triển khai**, bỏ qua các module chưa làm. Thay đổi kiến trúc Database thành **1 User - 1 Role**.

**Quy trình chuẩn cho mọi Task:**
1. **Test Design**: Trích xuất Business Rule → Xác định Blackbox (BVA, EP) → Tạo CSV Test Data.
2. **Unit Test**: Viết test với Mockito, đảm bảo 100% Branch Coverage (Whitebox).
3. **Integration Test**: Viết `@DataJpaTest` kết hợp Testcontainers để test DB Mapping, Constraints.
4. **Implementation**: Code Entity, Repository, Mapper, Service để pass test.

---

### Backend Phase 1: Database Schema & Identity Refactor
Sửa chữa DB Schema và các Entity đang sai lệch (Users, Roles, Permissions, Tokens, Notifications).

- **Task B-P1.0 (DB Schema)**: Cập nhật `pccms_database_schema.sql`. Thêm `role_id` vào bảng `users`. Xóa bỏ bảng `user_roles`. Chỉnh sửa câu lệnh `INSERT` cho seed data hợp lý.
- **Task B-P1.1 (Users Entity)**: Refactor `Users.java`. Giữ `@ManyToOne` với `Roles`. Đổi `userId` -> `id`. Thêm `phone`, `phoneVerifiedAt`, `lastLoginAt`, `updatedAt`, `deletedAt`. Xóa các trường thừa (`avatarUrl`, `bio`, `failedLoginAttempts`, `lockUntil`, v.v.) để khớp 100% với DB. Update logic Login/Register bị ảnh hưởng.
- **Task B-P1.2 (Roles & Permissions)**: Đổi tên bảng thành `roles`, `permissions`. Thêm `code`, `description`, `isActive`. Cập nhật bảng trung gian `role_permissions` (`role_id`, `permission_id`).
- **Task B-P1.3 (RefreshToken)**: Đổi bảng thành `refresh_tokens`. Thay `isRevoked` bằng `revokedAt`, thêm `issuedAt`, `revokedReason`.
- **Task B-P1.4 (Notification)**: Đập đi xây lại `EmailNotification` thành `Notification` map với bảng `notifications` trong Schema (dùng `recipient_user_id`, `title`, `body`). Sửa logic gửi email tương ứng.

---

### Frontend Phase 2: Frontend Sync & Alignment (Strict TDD)
Đồng bộ hóa Frontend DTOs và refactor lại Components để khớp 100% với Backend sau khi Backend đã chuyển sang kiến trúc 1 User - 1 Role.

- **Task F-P2.1 (Sync Models & DTOs)**: 
  - Cập nhật `frontend/src/types/user.ts`: 
    - Sửa `UserRole` enum thành `'ADMIN' | 'VETERINARIAN' | 'STAFF' | 'OWNER'`.
    - Refactor `UserResponse`: Xóa `avatarUrl`, `bio`, `roleName`, `userId`. Thêm `id`, `phone`, `roleCode`, `statusCode`.
  - Cập nhật `frontend/src/types/auth.ts`: Refactor `RegisterRequest` (xóa `phone` nếu backend ko yêu cầu hoặc chỉnh lại cho khớp).
  - Cập nhật các schemas trong `frontend/src/features/auth/api/authSchema.ts` (VD: `registerSchema` không gửi phone nếu ko cần).
- **Task F-P2.2 (Update Auth & MSW Mock)**: 
  - Sửa file `frontend/src/mocks/handlers.ts` để trả về `UserResponse` với schema mới (id, phone, roleCode, statusCode) cho API `/auth/me` hoặc khi login.
  - Sửa `frontend/src/app/contexts/AuthContext.tsx` để xử lý jwt payload role (`STAFF` thay vì `RECEPTIONIST`, `OWNER` thay vì `CUSTOMER`).
  - Viết/Fix Unit Tests (`AuthContext.test.tsx`, `authApi.test.ts`) để đảm bảo pass với mock data mới.
- **Task F-P2.3 (Refactor ProfilePage & Tests)**:
  - Sửa `frontend/src/features/owner/pages/ProfilePage.tsx`:
    - Xóa các logic liên quan đến upload/hiển thị `avatarUrl` cá nhân. (Có thể dùng UI placeholder Avatar dựa trên tên).
    - Thay `profile.userId` thành `profile.id`.
    - Xóa field `bio` khỏi form update profile (nếu có).
  - Fix Unit Tests cho `ProfilePage` (`ProfilePage.test.tsx`): Sửa các mock api trả về profile và simulate việc update không gửi `avatarUrl`/`bio`.
- **Task F-P2.4 (Refactor DashboardLayout & Routes)**:
  - Đảm bảo `DashboardLayout.tsx` hiển thị navigation đúng role (kiểm tra các role guard dựa trên `OWNER`, `STAFF`).
  - Sửa `App.tsx` hoặc các `RoleRoute` component để chấp nhận các roles mới.
  - Chạy toàn bộ test suites (`pnpm test`) ở Frontend để đảm bảo tỷ lệ pass 100%.
