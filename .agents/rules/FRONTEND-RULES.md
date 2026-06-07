---
trigger: always_on
---

# [FRONTEND-RULES] QUY CHUẨN LẬP TRÌNH FRONTEND (REACTJS + VITE)

> **MỤC ĐÍCH:** Tài liệu này thiết lập tiêu chuẩn kỹ thuật bắt buộc cho tầng Frontend của dự án PCCMS. AI Agent **PHẢI** tuân thủ mọi quy tắc ở đây khi sinh code React, đảm bảo hiệu năng cao, dễ bảo trì và UX/UI đồng nhất.

---

## 1. TECH STACK BẮT BUỘC (MÔI TRƯỜNG CÔNG NGHỆ)

- **Core:** ReactJS (Functional Components, Hooks), Vite.
- **Language:** TypeScript (Ưu tiên) hoặc JavaScript ES6+.
- **Data Fetching & Server State:** `Axios` kết hợp với `React Query` (hoặc `SWR`).
- **Form & Validation:** `React Hook Form` kết hợp với `Yup` hoặc `Zod`.
- **Routing:** `React Router DOM` v6+.

---

## 2. KIẾN TRÚC THƯ MỤC (FEATURE-SLICED DESIGN - FSD)

Tuyệt đối KHÔNG gộp chung tất cả components vào một thư mục `src/components`. Cấu trúc mã nguồn BẮT BUỘC tuân thủ FSD:

```text
src/
├── app/                  # Thiết lập ứng dụng (Providers, Router gốc, Global Styles)
├── pages/                # Các trang (Page). Ví dụ: MedicalRecordPage.tsx. CHỈ ĐÓNG VAI TRÒ CONTAINER.
├── features/             # Các khối nghiệp vụ (Domain-driven). Ví dụ: pet-management, billing.
│   └── pet-management/
│       ├── api/          # Các hàm Axios gọi API liên quan đến Pet
│       ├── components/   # UI Components riêng của Pet (Ví dụ: PetCard, PetForm)
│       └── store/        # Local state của riêng Pet (nếu có)
├── shared/               # Code dùng chung toàn hệ thống
│   ├── api/              # Axios Client (Interceptors)
│   ├── components/       # UI Dùng chung (Button, Modal, Table, Toast)
│   ├── hooks/            # Custom hooks dùng chung (useAuth, useDebounce)
│   └── utils/            # Formatters (currency, date)

```

---

## 3. QUY TRÌNH FETCHING DỮ LIỆU & GỌI API (DATA FETCHING)

### 3.1. Quy tắc Thép: Không lạm dụng `useEffect`

- **CẤM** gọi API trực tiếp bên trong `useEffect` và lưu vào `useState` một cách thủ công (gây lỗi race condition và re-render thừa).
- **Bắt buộc:** Dùng thư viện quản lý Server State (như `React Query`) hoặc Custom Hooks được bọc sẵn cẩn thận để tự động xử lý `isLoading`, `isError`, và `caching`.

### 3.2. Cấu hình Axios & Interceptors

- Mọi API call phải đi qua một `axiosClient` duy nhất đặt tại `src/shared/api/axiosClient.ts`.
- **Interceptor Request:** Tự động đính kèm header `Authorization: Bearer <token>` (lấy từ LocalStorage hoặc Zustand).
- **Interceptor Response:** - Tự động bóc tách dữ liệu từ cấu trúc bọc `ApiResponse` của Backend (`return response.data.data`).
- Bắt lỗi tập trung: Nếu API trả về `401 Unauthorized`, tự động gọi hàm logout và đẩy về trang đăng nhập (`/login`). Nếu `403`, hiện Toast "Không có quyền truy cập".

---

## 4. QUẢN LÝ FORM & CLIENT-SIDE VALIDATION

### 4.1. React Hook Form

- Mọi Form nhập liệu (Đăng ký, Thêm thú cưng, Kê đơn thuốc) **BẮT BUỘC** dùng `React Hook Form` để quản lý state (chống re-render toàn bộ component mỗi lần gõ phím).
- **Tuyệt đối không:** Dùng `onChange` và `useState` thủ công cho từng input (VD: `const [name, setName] = useState('')`).

### 4.2. Zod / Yup Validation

- Mọi luật kiểm tra dữ liệu phải được định nghĩa bằng Schema (Zod hoặc Yup).
- Frontend phải chặn lỗi ngay tại trình duyệt trước khi gửi Request lên Backend.
- _Ví dụ:_ SpO2 phải từ 0-100, Nhiệt độ phải hợp lệ, Không để trống tên Thú cưng.

- Bắt buộc hiển thị dòng text lỗi (màu đỏ) ngay dưới input bị sai.

---

## 5. QUY CHUẨN UI/UX VÀ XỬ LÝ TRẠNG THÁI (STATE HANDLING)

### 5.1. Luật 3 Trạng thái (The 3-State Rule)

Bất kỳ Component nào gọi API lấy dữ liệu cũng phải thiết kế UI để cover đủ 3 trạng thái:

1. **Loading:** Hiện Spinner hoặc Skeleton Loading. Tuyệt đối không để màn hình trắng.
2. **Error:** Hiện thông báo lỗi thân thiện (VD: "Không thể tải danh sách thú cưng, vui lòng thử lại"). Nút Retry (nếu cần).
3. **Success (Empty & Data):** Nếu không có dữ liệu (mảng rỗng), hiện màn hình "Không có dữ liệu / Empty State". Nếu có, render danh sách.

### 5.2. Chống thao tác kép (Double-Submit Prevention)

- Các nút submit Form (Lưu bệnh án, Thanh toán) **BẮT BUỘC** phải bị `disabled` và hiện icon quay (loading) khi đang chờ phản hồi từ Backend.

---

## 6. PHÂN QUYỀN GIAO DIỆN (ROLE-BASED UI & ROUTING)

### 6.1. Protected Routes (Bảo vệ Route)

- Màn hình nào cần đăng nhập, Agent phải bọc trong một component `<ProtectedRoute>`. Nếu chưa có token, `Maps` về `/login`.
- Màn hình dành riêng cho vai trò cụ thể (VD: Quản lý User chỉ dành cho ADMIN), bọc trong `<RoleRoute allowedRoles={['ADMIN']}>`.

### 6.2. UI Locking (Khóa giao diện theo nghiệp vụ)

Đặc tả nghiệp vụ Y tế (Medical Care) của PCCMS yêu cầu khắt khe về tính toàn vẹn dữ liệu:

- Khi Fetch `MedicalRecord`, nếu `statusCode === 'FINALIZED'`, Agent **BẮT BUỘC** phải render toàn bộ Input ở trạng thái `disabled={true}`, đồng thời ẩn/xóa toàn bộ các nút `Save`, `Update`, `Delete` trên màn hình.

---

## 7. QUY TẮC COMPONENT (REACT BEST PRACTICES)

- **Tên Component:** Viết hoa chữ cái đầu (`PascalCase`). Ví dụ: `PetProfileCard`.
- **Tách nhỏ Component:** Một file Component không nên dài quá 300 dòng. Nếu Form quá dài (như Bệnh án), phải tách nhỏ thành `<VitalSignsForm />`, `<PrescriptionForm />`.
- **Tránh Magic Strings:** Tên các Route (đường dẫn URL) phải được lưu trong một file hằng số (VD: `ROUTES.PET_LIST` thay vì hardcode `'/pets'`).

> **XÁC NHẬN CỦA AGENT:** Bằng việc nạp file này, Agent cam kết viết code React theo chuẩn FSD, dùng React Hook Form + Schema Validation cho mọi Form, và luôn quản lý đủ 3 trạng thái Loading/Error/Success khi gọi API.
