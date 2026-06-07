# PCCMS Frontend Spec — Kết nối Backend

> **Version:** 1.0.0 | **Last updated:** 2026-06-03
> **Phạm vi:** Toàn bộ giao diện React (Vite + TypeScript) kết nối với backend Spring Boot 4 đã hoàn thiện.

---

## 1. Mục tiêu & Đối tượng người dùng

### 1.1 Mục tiêu

Xây dựng giao diện quản lý trung tâm chăm sóc thú cưng (PCCMS) — một **web app nghiệp vụ full-featured** — kết nối trực tiếp với backend Java/Spring Boot đã có. Giao diện thay thế toàn bộ mock data bằng API calls thực.

### 1.2 Đối tượng người dùng (4 Roles)

| Role | Màn hình chính | Ưu tiên UX |
|---|---|---|
| **CUSTOMER** (Chủ nuôi) | Dashboard, Pets, Booking, Payments, Profile | Mobile-first, đơn giản |
| **VETERINARIAN** (Bác sĩ) | Doctor Queue, Medical Record, Prescription | Tốc độ thao tác, UI lock FINALIZED |
| **RECEPTIONIST** | Appointments, Grooming Board, Boarding Log | Task-oriented, tích hợp nhiều view |
| **ADMIN** | Accounts, Medicines, Reports | Data-heavy, bảng biểu, phân quyền |

---

## 2. Tech Stack (Hiện tại & Bổ sung)

### 2.1 Đã có (giữ nguyên)

```
React 19 + Vite 8 + TypeScript 6
Tailwind CSS v4 (@tailwindcss/vite)
Axios (src/api/api.ts — đã có Interceptors JWT)
React Router DOM v7
Motion (Framer Motion)
Lucide React (icons — đã dùng trong dự án)
```

### 2.2 CẦN CÀI THÊM (bắt buộc)

```bash
# Server State Management
pnpm add @tanstack/react-query

# Form & Validation
pnpm add react-hook-form zod @hookform/resolvers

# UI Notification
pnpm add react-hot-toast
```

### 2.3 KHÔNG được thêm

- Không thêm MUI, Ant Design, Chakra UI (xung đột design system)
- Không thêm Redux (đã có React Query + Context)
- Không thêm H2 database hoặc bất kỳ mock server nào

---

## 3. Cấu trúc thư mục (Feature-Sliced Design)

```text
src/
├── app/                          # Providers toàn cục
│   ├── providers.tsx             # QueryClient, Toast, Auth Context
│   └── router.tsx                # (move từ src/router/index.tsx)
│
├── shared/                       # Code dùng chung (NEW)
│   ├── api/
│   │   ├── axiosClient.ts        # Refactor từ src/api/api.ts
│   │   └── queryKeys.ts          # React Query key factory
│   ├── components/
│   │   ├── ui/                   # Primitive: Button, Badge, Modal, Table
│   │   ├── DataTable/            # Bảng dữ liệu có sort/filter/pagination
│   │   ├── EmptyState/           # Component "Không có dữ liệu"
│   │   ├── ErrorState/           # Component lỗi + retry
│   │   └── SkeletonLoader/       # Loading skeleton
│   ├── hooks/
│   │   ├── useAuth.ts            # Lấy user hiện tại từ JWT
│   │   └── useDebounce.ts        # Debounce search input
│   └── utils/
│       ├── formatters.ts         # Format ngày, tiền tệ, trạng thái
│       └── errorHandlers.ts      # Parse lỗi từ ApiResponse
│
├── features/                     # Giao diện theo nghiệp vụ
│   ├── auth/                     # Login, Register, ForgotPassword
│   │   ├── api/authApi.ts        # POST /auth/login, /auth/register
│   │   ├── hooks/useLogin.ts
│   │   └── pages/               # (đã có — chỉ wire vào real API)
│   │
│   ├── owner/                    # Role CUSTOMER
│   │   ├── api/
│   │   │   ├── petApi.ts         # GET/POST /api/v1/pets
│   │   │   └── userApi.ts        # GET/PUT /users/me
│   │   ├── components/
│   │   └── pages/               # (đã có — wire vào API)
│   │
│   ├── doctor/                   # Role VETERINARIAN
│   │   ├── api/
│   │   │   ├── medicalRecordApi.ts   # GET/PUT /api/v1/medical-records/{id}
│   │   │   └── prescriptionApi.ts   # POST /api/v1/medical-records/{id}/prescriptions
│   │   ├── components/
│   │   │   ├── VitalSignsForm/      # Form chỉ số sinh tồn (với validation)
│   │   │   └── PrescriptionTable/   # Bảng kê đơn dynamic add/remove
│   │   └── pages/
│   │
│   ├── admin/                    # Role ADMIN
│   │   ├── api/
│   │   │   ├── userAdminApi.ts   # POST/PUT/DELETE/PATCH /users
│   │   │   └── medicineApi.ts    # CRUD /api/v1/medicines
│   │   └── pages/
│   │
│   └── reception/                # Role RECEPTIONIST
│       └── pages/
│
├── pages/                        # Container pages (chỉ import từ features)
├── components/                   # (giữ nguyên: atoms, molecules, layouts)
├── constants/
│   ├── auth.ts                   # (cập nhật — loại bỏ mockAuth sau khi wiring)
│   ├── roles.ts
│   ├── routes.ts                 # NEW: tập trung tất cả path string
│   └── screenMeta.ts
├── types/
│   ├── navigation.ts
│   ├── api.ts                    # NEW: ApiResponse<T>, PageResponse<T>
│   ├── user.ts                   # NEW: User, UserRole
│   ├── pet.ts                    # NEW: Pet, PetRequest
│   └── medicalRecord.ts          # NEW: MedicalRecord, RecordStatus
└── hooks/
```

---

## 4. API Contract — Backend đã có

> Mọi API call phải bọc trong `ApiResponse<T>` từ backend:
> ```json
> { "success": true, "code": 200, "message": "...", "data": { ... } }
> ```
> Interceptor trong `axiosClient.ts` phải unwrap `response.data.data` tự động.

### 4.1 Auth Module (`/api/v1/auth`)

| Method | Endpoint | Request | Response | Dùng ở |
|---|---|---|---|---|
| POST | `/auth/login` | `{email, password}` | `{token, refreshToken, role}` | LoginPage |
| POST | `/auth/register` | `{fullName, email, phone, password}` | `UserResponse` | RegisterPage |
| POST | `/auth/refresh` | cookie/header | `{token}` | axiosClient (auto) |
| POST | `/auth/logout` | — | — | Header logout |

### 4.2 User Module (`/api/v1/users`)

| Method | Endpoint | Role | Dùng ở |
|---|---|---|---|
| GET | `/users/me` | All | ProfilePage, Header Avatar |
| PUT | `/users/me` | All | ProfilePage (chỉnh sửa) |
| PUT | `/users/me/password` | All | ProfilePage (đổi mật khẩu) |
| GET | `/users` | ADMIN | AccountsPage (danh sách) |
| POST | `/users` | ADMIN | AccountsPage (tạo mới) |
| PUT | `/users/{userId}` | ADMIN | AccountsPage (sửa) |
| PATCH | `/users/{userId}/lock` | ADMIN | AccountsPage (khóa) |
| PATCH | `/users/{userId}/disable` | ADMIN | AccountsPage (vô hiệu) |
| DELETE | `/users/{userId}` | ADMIN | AccountsPage (xóa mềm) |

### 4.3 Pet Module (`/api/v1/pets`)

| Method | Endpoint | Role | Dùng ở |
|---|---|---|---|
| POST | `/api/v1/pets` | CUSTOMER | PetProfilesPage (thêm thú cưng) |
| GET | `/api/v1/pets/{petId}` | CUSTOMER, VET | PetProfilesPage (hồ sơ) |
| PUT | `/api/v1/pets/{petId}` | CUSTOMER | PetProfilesPage (sửa) |

### 4.4 Medical Record Module (`/api/v1/medical-records`)

| Method | Endpoint | Role | Dùng ở |
|---|---|---|---|
| PUT | `/api/v1/medical-records/{id}` | VET | MedicalRecordPage (cập nhật) |
| PATCH | `/api/v1/medical-records/{id}/finalize` | VET | MedicalRecordPage (chốt bệnh án) |

**Business Rule bắt buộc implement:**
- Khi `record.recordStatus === "FINALIZED"` → toàn bộ input `disabled`, ẩn nút Save/Finalize.

### 4.5 Prescription Module (`/api/v1/medical-records/{id}/prescriptions`)

| Method | Endpoint | Role | Dùng ở |
|---|---|---|---|
| POST | `/api/v1/medical-records/{id}/prescriptions` | VET | PrescriptionTable (kê đơn) |

### 4.6 Medicine Module (`/api/v1/medicines`)

| Method | Endpoint | Role | Dùng ở |
|---|---|---|---|
| GET | `/api/v1/medicines` | VET, ADMIN | PrescriptionTable (autocomplete), CatalogPage |
| GET | `/api/v1/medicines/{id}` | VET, ADMIN | Chi tiết thuốc |
| POST | `/api/v1/medicines` | ADMIN | CatalogPage (thêm) |
| PUT | `/api/v1/medicines/{id}` | ADMIN | CatalogPage (sửa) |
| PATCH | `/api/v1/medicines/{id}/stock` | ADMIN | CatalogPage (nhập kho) |
| DELETE | `/api/v1/medicines/{id}` | ADMIN | CatalogPage (xóa) |

---

## 5. TypeScript Types (Contract với Backend DTOs)

```typescript
// src/types/api.ts
export interface ApiResponse<T> {
  success: boolean;
  code: number;
  message: string;
  data: T;
  errorCode?: string;
  errors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  isLast: boolean;
}

// src/types/user.ts
export type UserRole = "ADMIN" | "VETERINARIAN" | "RECEPTIONIST" | "CUSTOMER";
export type UserStatus = "ACTIVE" | "INACTIVE" | "LOCKED";

export interface UserResponse {
  userId: string;
  fullName: string;
  email: string;
  role: UserRole;
  statusCode: UserStatus;
  createdAt: string;
}

// src/types/pet.ts
export interface PetResponse {
  id: string;
  petName: string;
  species: string;
  breed?: string;
  gender?: string;
  birthDate?: string;
  weight?: number;
  isNeutered?: boolean;
  isActive: boolean;
  healthAlerts: HealthAlertResponse[];
}

export interface HealthAlertResponse {
  id: string;
  alertType: string;
  message: string;
  severity: string;
  isResolved: boolean;
}

// src/types/medicalRecord.ts
export type RecordStatus = "DRAFT" | "FINALIZED";

export interface MedicalRecordResponse {
  id: string;
  recordCode: string;
  petId: string;
  vetId: string;
  recordStatus: RecordStatus;
  temperatureC?: number;
  heartRateBpm?: number;
  respiratoryRateBpm?: number;
  spo2Percent?: number;
  weightKg?: number;
  bloodPressure?: string;
  mucousMembraneColor?: string;
  capillaryRefillSeconds?: number;
  preliminaryDiagnosis?: string;
  finalDiagnosis?: string;
  treatmentNote?: string;
  followUpAt?: string;
  lockedAt?: string;
}

// src/types/medicine.ts
export interface MedicineResponse {
  id: string;
  medicineName: string;
  unit: string;
  currentStock: number;
  unitPrice: number;
  isActive: boolean;
}
```

---

## 6. Axio Client & State Management

### 6.1 Refactor axiosClient.ts

```typescript
// src/shared/api/axiosClient.ts
// - Unwrap response.data.data tự động
// - 401 → logout + redirect /login
// - 403 → toast "Không có quyền truy cập"
// - 503 → maintenance mode
```

### 6.2 React Query Setup

```typescript
// src/app/providers.tsx
// QueryClient với staleTime: 30s, retry: 1
// Wrap QueryClientProvider + Toaster
```

### 6.3 Query Key Factory

```typescript
// src/shared/api/queryKeys.ts
export const queryKeys = {
  users: { all: ['users'], detail: (id: string) => ['users', id] },
  pets: { all: ['pets'], detail: (id: string) => ['pets', id] },
  medicalRecords: { detail: (id: string) => ['medical-records', id] },
  medicines: { all: ['medicines'], detail: (id: string) => ['medicines', id] },
};
```

---

## 7. Yêu cầu Validation (Zod Schemas)

### 7.1 Medical Vital Signs

```typescript
const vitalSignsSchema = z.object({
  temperatureC: z.number().min(30).max(45).optional(),
  heartRateBpm: z.number().int().min(30).max(300).optional(),
  respiratoryRateBpm: z.number().int().min(5).max(80).optional(),
  spo2Percent: z.number().int().min(0).max(100).optional(),
  weightKg: z.number().min(0.01).max(200).optional(),
});
```

### 7.2 Prescription Row

```typescript
const prescriptionItemSchema = z.object({
  medicineId: z.string().uuid("Chọn thuốc hợp lệ"),
  dosage: z.number().positive("Liều lượng phải > 0"),
  frequency: z.string().min(1, "Tần suất không được trống"),
  durationDays: z.number().int().min(1).max(365),
  // Auto-calculate: totalQuantity = dosage * frequency * durationDays
  instructions: z.string().optional(),
});
```

### 7.3 Pet Profile

```typescript
const petSchema = z.object({
  petName: z.string().min(1, "Tên thú cưng không được trống").max(100),
  species: z.string().min(1, "Chọn loài"),
  breed: z.string().optional(),
  birthDate: z.string().optional(),
  weight: z.number().positive().optional(),
});
```

---

## 8. UI/UX Standards

### 8.1 Design Read

> **"Reading this as: Enterprise pet-care management web app for multiple staff roles, with a clean B2B dashboard aesthetic, leaning toward Tailwind v4 + sidebar-dominant layout + restrained motion."**

**Dials:**
- `DESIGN_VARIANCE: 5` (B2B enterprise, không avant-garde)
- `MOTION_INTENSITY: 4` (Subtle transitions, không theatrical)
- `VISUAL_DENSITY: 6` (Data-heavy dashboard, nhiều bảng/form)

### 8.2 Layout

- **Authenticated app:** Header (search, notification, profile) + Sidebar (module navigation) + Content area.
- **Auth pages:** Centered card layout.
- **Sidebar:** Collapsible, 240px expanded / 64px collapsed, mobile → drawer.

### 8.3 Color Palette (Tailwind v4 Tokens)

```css
/* Không dùng "AI purple". Dùng clean neutral + teal accent */
--color-primary: oklch(0.55 0.15 195); /* Teal/cyan accent */
--color-surface: oklch(0.99 0 0);
--color-surface-raised: oklch(0.97 0 0);
--color-text: oklch(0.15 0 0);
--color-text-muted: oklch(0.45 0 0);
--color-border: oklch(0.88 0 0);
--color-success: oklch(0.55 0.15 150);
--color-warning: oklch(0.65 0.18 65);
--color-error: oklch(0.55 0.22 25);
```

### 8.4 Typography

- Font chính: `Geist` (sans) — **không dùng Inter mặc định**.
- Code/mono: `Geist Mono`
- Body: 15px / line-height 1.6
- H1: 28-32px, H2: 22-24px, H3: 18-20px

### 8.5 The 3-State Rule (BẮT BUỘC mọi component gọi API)

1. **Loading:** Skeleton matching final layout shape (không dùng spinner đơn độc)
2. **Error:** Message thân thiện + nút retry
3. **Success Empty:** Empty state có hướng dẫn hành động tiếp theo

### 8.6 UI Lock cho Medical Record (BẮT BUỘC)

```tsx
const isLocked = record.recordStatus === 'FINALIZED';
// Tất cả input: disabled={isLocked}
// Ẩn toàn bộ: nút Save, Finalize, Add Medicine Row
// Hiện badge: "Đã chốt" (màu teal/green, read-only)
```

---

## 9. Auth Flow (Thay thế mockAuth)

### 9.1 JWT Storage & Context

```typescript
// src/shared/hooks/useAuth.ts
// - Lấy token từ localStorage
// - Decode JWT lấy role, userId, email
// - Expose: { user, role, isAuthenticated, logout }
```

### 9.2 Protected Route

```tsx
// Thay AuthGuard hiện tại bằng real JWT check
// Nếu token expired → redirect /login
// Nếu role sai → redirect về role home của user thật
```

### 9.3 Logout

```typescript
// 1. Gọi POST /auth/logout
// 2. Xóa localStorage token
// 3. Clear React Query cache
// 4. Navigate /login
```

---

## 10. Chuẩn Component

### 10.1 Atomic Design trong PCCMS

```
components/atoms/    → Button, Badge, Input, Spinner, Avatar
components/molecules/ → FormField, SearchBar, StatCard, AlertBanner
components/layouts/  → DashboardLayout, AuthLayout, PageHeader
features/*/components/ → Domain-specific (PetCard, VitalSignsForm, PrescriptionTable)
```

### 10.2 Double-Submit Prevention

```tsx
<Button
  type="submit"
  disabled={isSubmitting}
  loading={isSubmitting}
>
  {isSubmitting ? "Đang lưu..." : "Lưu bệnh án"}
</Button>
```

### 10.3 Error Display (Form)

```tsx
// Hiển thị lỗi ngay dưới input — màu error, font-size 13px
{errors.petName && (
  <p className="text-error text-sm mt-1">{errors.petName.message}</p>
)}
```

---

## 11. Route Constants

```typescript
// src/constants/routes.ts
export const ROUTES = {
  LOGIN: '/login',
  REGISTER: '/register',
  FORGOT_PASSWORD: '/forgot-password',

  OWNER: {
    DASHBOARD: '/owner',
    PETS: '/owner/pets',
    PET_DETAIL: (id: string) => `/owner/pets/${id}`,
    BOOK: '/owner/book',
    BOARDING: '/owner/boarding/tracking',
    PAYMENTS: '/owner/payments',
    PROFILE: '/owner/profile',
  },

  DOCTOR: {
    DASHBOARD: '/doctor',
    QUEUE: '/doctor/queue',
    MEDICAL_RECORD: (id: string) => `/doctor/medical-record/${id}`,
  },

  RECEPTION: {
    DASHBOARD: '/reception',
    APPOINTMENTS: '/reception/appointments',
    GROOMING: '/reception/grooming-board',
    BOARDING_LOG: '/reception/boarding-log',
  },

  ADMIN: {
    DASHBOARD: '/admin',
    ACCOUNTS: '/admin/accounts',
    CATALOG: '/admin/catalog',
    ROOMS: '/admin/rooms',
    SCHEDULE: '/admin/schedule',
    REPORTS: '/admin/reports',
  },
} as const;
```

---

## 12. Kế hoạch Triển khai (Slices)

### Slice A — Foundation (Prerequisite cho mọi slice)

- [ ] Cài thêm: `@tanstack/react-query`, `react-hook-form`, `zod`, `@hookform/resolvers`, `react-hot-toast`
- [ ] Tạo `src/shared/` theo FSD structure
- [ ] Refactor `axiosClient.ts` → unwrap `data.data`, handle 401/403/503
- [ ] Tạo `src/types/api.ts`, `user.ts`, `pet.ts`, `medicalRecord.ts`, `medicine.ts`
- [ ] Setup React Query Provider + Toaster trong `src/app/providers.tsx`
- [ ] Tạo `queryKeys.ts`
- [ ] Tạo Shared UI components: Button, Badge, DataTable, EmptyState, SkeletonLoader, ErrorState
- [ ] Tạo `src/constants/routes.ts`

### Slice B — Auth (Replace mockAuth)

- [ ] `src/features/auth/api/authApi.ts` → real `/auth/login`, `/auth/register`
- [ ] `src/shared/hooks/useAuth.ts` → JWT decode, AuthContext
- [ ] Cập nhật `AuthGuard` → real JWT check
- [ ] Cập nhật `LoginPage` → React Hook Form + Zod + useMutation
- [ ] Cập nhật `RegisterPage` → React Hook Form + Zod + useMutation
- [ ] Logout flow: POST /auth/logout → clear cache → navigate

### Slice C — User/Profile

- [ ] `GET /users/me` → `ProfilePage`
- [ ] `PUT /users/me` → Chỉnh sửa profile
- [ ] `PUT /users/me/password` → Đổi mật khẩu
- [ ] Header Avatar → hiển thị tên/avatar thực

### Slice D — Pet Management (Owner)

- [ ] `POST /api/v1/pets` → Thêm thú cưng
- [ ] `GET /api/v1/pets/{petId}` → Hồ sơ thú cưng (hiển thị HealthAlerts)
- [ ] `PUT /api/v1/pets/{petId}` → Sửa thú cưng
- [ ] `PetProfilesPage` → list + detail

### Slice E — Medical Record & Prescription (Doctor)

- [ ] `MedicalRecordPage` → React Hook Form + VitalSignsSchema
- [ ] `PUT /api/v1/medical-records/{id}` → Auto-save hoặc manual save
- [ ] `PATCH /api/v1/medical-records/{id}/finalize` → UI lock sau finalize
- [ ] `PrescriptionTable` → dynamic add/remove rows, autocomplete medicine
- [ ] `POST /api/v1/medical-records/{id}/prescriptions` → submit đơn thuốc

### Slice F — Medicine Catalog (Admin)

- [ ] `GET /api/v1/medicines` → Danh sách thuốc + phân trang
- [ ] CRUD operations → Modal form (tạo/sửa)
- [ ] `PATCH /api/v1/medicines/{id}/stock` → Nhập kho

### Slice G — Account Management (Admin)

- [ ] `GET /users` → Danh sách tài khoản với filter theo role/status
- [ ] CRUD + lock/disable → Confirmation dialogs

---

## 13. Tiêu chuẩn Hoàn thành (DoD)

Một Slice được xem là hoàn thành khi:

- [ ] Tất cả API call dùng React Query (không dùng useEffect + setState raw)
- [ ] Tất cả form dùng React Hook Form + Zod validation
- [ ] Implement đủ 3 trạng thái: Loading (Skeleton), Error, Success/Empty
- [ ] UI Lock cho Medical Record FINALIZED
- [ ] Double-submit prevention trên mọi form submit button
- [ ] Error message từ backend (`ApiResponse.message`) hiển thị qua Toast
- [ ] Không còn hardcode string path (dùng `ROUTES` constants)
- [ ] TypeScript — không có `any` type tùy tiện
- [ ] Responsive: hoạt động đúng trên mobile 375px và desktop 1440px
- [ ] Không có console errors

---

## 14. Ranh giới (Boundaries)

### Luôn làm

- Wrap mọi response trong `ApiResponse<T>` type
- Dùng `queryKeys` factory cho tất cả query keys
- Hiển thị message lỗi từ backend (không chỉ generic "có lỗi")
- Kiểm tra role từ JWT trước khi render UI nhạy cảm

### Hỏi trước khi làm

- Thêm feature mới chưa có API backend tương ứng
- Thay đổi design system (màu sắc, font)
- Tích hợp thư viện UI mới (Radix, shadcn/ui)

### Không bao giờ làm

- Gọi API trực tiếp trong `useEffect` mà không qua React Query
- Dùng `useState` thủ công cho từng form input
- Hardcode `userId` hay `accountId` trong request body (phải lấy từ JWT)
- Để màn hình trắng khi loading hoặc error
- Để nút Submit không bị disable khi đang pending
- Commit mock data (`mockAuth`, hardcoded arrays) lên main branch sau khi wiring

---

*SPEC này phải được review và approve trước khi bắt đầu Slice A.*
