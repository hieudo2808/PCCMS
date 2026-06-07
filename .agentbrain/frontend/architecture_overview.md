# Frontend Architecture Overview

Tài liệu này lưu trữ toàn bộ trạng thái, cấu trúc thư mục và quy tắc làm việc cho Frontend của dự án **Pet Care Center Management System (PCCMS)**. Cần tuân thủ chặt chẽ các file và quy ước dưới đây để giữ hệ thống clean.

---

## 1. Công nghệ & Dependencies

- **Core**: Vite, React 18, TypeScript.
- **Styling**: Tailwind CSS v4 (cấu hình plugin vite thay vì postcss).
- **Icons**: `lucide-react`.
- **Animations / Tương tác**: `motion` (Framer Motion).
- **Routing**: `react-router-dom` (sử dụng `createBrowserRouter` và `Outlet` cho nested routes).
- **HTTP/API**: `axios` (đã config global interceptor với custom types trong `api.ts`).

---

## 2. Mô hình kiến trúc (Clean Architecture + Atomic Design)

Dự án áp dụng mô hình thiết kế cấu trúc phân nhánh kết hợp Atomic Design ở tầng UI để tối đa hóa tái sử dụng.

### Cấu trúc thư mục lõi
```text
src/
├── api/                  # API Layer: axios config, interceptors, error handling.
├── components/           # UI Components (Atomic Design).
│   ├── atoms/            # Nền tảng: Button, Input, Select, Tag...
│   ├── molecules/        # Tổ hợp: Card, DataTable, Stat, Notice...
│   └── layouts/          # Layouts gốc: AuthLayout, DashboardLayout, TopBar.
├── constants/            # Dữ liệu tĩnh: roles config, screenMeta (icons, path).
├── features/             # Phân hệ theo Domain (Domain-driven).
│   ├── admin/            # Dashboard, Tài khoản, Catalog, Báo cáo...
│   ├── auth/             # Login, Register, Forgot Password...
│   ├── doctor/           # Queue chờ khám, Bệnh án, Đơn thuốc...
│   ├── owner/            # Book dịch vụ, Lịch sử hóa đơn, Thú cưng...
│   └── reception/        # Lịch hẹn, Kanban Grooming, Boarding Log...
├── router/               # Cấu hình routing trung tâm `createBrowserRouter`.
├── types/                # TypeScript Interfaces, Types map cho toàn bộ app.
└── utils/                # Tiện ích bổ trợ: cx.ts (nối class).
```

---

## 3. Quy ước Lập Trình (Coding Standards)

1. **Separation of Concerns**: Phân tán rạch ròi. Page components chỉ map cấu trúc lớn. CSS ném hết vào Tailwind class với hàm `cx()` giúp dễ quản lý. Style nào dùng ở 2 nơi trở lên -> gom thành atom/molecule components.
2. **Không Sidebar, ưu tiên không gian rộng**: Dashboard Layout hiện dùng mô hình **Header-Only (TopBar)** chứa thanh công cụ và tab menu để tận dụng toàn bộ chiều rộng cho bảng biểu.
3. **TypeScript Strictly**: Mọi component, props, API map phải có Interface rõ ràng, nghiêm cấm `any` nếu không bắt buộc.
4. **Mockup Data Strategy**: UI đang dùng mockup data thẳng trong file để demo luồng tương tác. Giai đoạn tích hợp sẽ dùng `react-query` gắn vào các Hook call nằm trong `src/features/[feature]/api/`.

---

## 4. UI Components hiện có (Danh mục tra cứu)

Khi cần code giao diện mới, hãy ưu tiên dùng lại các components này thay vì code chay DOM chuẩn:

### Atoms (`src/components/atoms`)
- `Button`: Hỗ trợ variants (`primary`, `secondary`, `ghost`, `soft`, `outline`).
- `Input` & `Textarea`: Form core components.
- `Select`: Dropdown.
- `Tag`: Badges highlight có tông màu (`green`, `amber`, `red`, `blue`).

### Molecules (`src/components/molecules`)
- `Card`: Container bo viền chủ đạo, gán tiêu đề tự động.
- `DataTable`: Generic mapping component cho rows & cols.
- `Stat`, `MiniGridStats`: Cụm hiển thị con số thống kê trên Dashboard.
- `Notice`, `AlertCard`: Container cảnh báo có màu.
- `SummaryRow`, `TimeSlot`: Hiển thị chi tiết thời gian và tóm tắt dịch vụ.
- `Vital`: Thẻ hiển thị chỉ số sinh tồn (Có cảnh báo `abnormal`).

### Layouts (`src/components/layouts`)
- `TopBar`: Header tổng, điều hướng route.
- `DashboardLayout`: Wrapper component tự bao phủ TopBar cho app.
- `AuthLayout`: Split-screen layout (Bên trái là brand, bên phải là form) chuyên trị auth.

---

## 5. Danh sách 23+ Screens được hỗ trợ

| Tên Module | Trang (Routes) |
|---|---|
| **Auth** | Login (`/login`), Register (`/register`), Forgot Password (`/forgot-password`). |
| **Owner** | Dashboard (`/owner`), Pets (`/owner/pets`), Appt Booking (`/owner/appointments/book`), Boarding Booking (`/owner/boarding/book`), Grooming Booking (`/owner/grooming/book`), Tracking (`/owner/boarding/tracking`), Payments (`/owner/payments`), Profile (`/owner/profile`). |
| **Reception** | Cổng điều phối (`/reception`), Màn đón tiếp (`/reception/appointments`), Kanban xử lý Spa (`/reception/grooming-board`), Sổ theo dõi Lưu trú (`/reception/boarding-log`). |
| **Doctor** | Dashboard chỉ số (`/doctor`), Hàng chờ khám (`/doctor/queue`), Màn hình chẩn đoán & kê đơn (`/doctor/medical-record`). |
| **Admin** | Dashboard Tổng quan (`/admin`), Quản lý tài khoản (`/admin/accounts`), Danh mục giá/thuốc (`/admin/catalog`), Kho phòng (`/admin/rooms`), Xếp lịch trực (`/admin/schedule`), Báo cáo doanh thu (`/admin/reports`). |

---

> Trạng thái hiện tại: Hoàn thiện Phase 1 - Dựng toàn bộ kiến trúc frontend modular và layout tĩnh theo mẫu. Dev server đang hoạt động mà không vướng Error. Phase tiếp theo có thể thực hiện Tích hợp API thật thay cho lớp Mock data hiện tại.
