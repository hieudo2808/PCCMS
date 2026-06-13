# PCCMS - Pet Care Center Management System

Hệ thống quản lý trung tâm chăm sóc thú cưng chuyên nghiệp, cung cấp nền tảng quản lý toàn diện từ đặt lịch khám, khám chữa bệnh, spa (grooming), lưu trú (boarding) đến thanh toán.

## Công nghệ sử dụng

| Thành phần   | Công nghệ                      | Mô tả                                            |
| ------------ | ------------------------------ | ------------------------------------------------ |
| **Frontend** | React 19 + Vite 8 + TypeScript | UI mượt mà, render nhanh với Vite                |
| **Backend**  | Java 25 + Spring Boot 4.0.5    | Kiến trúc Modular Monolith, Layered Architecture |
| **Database** | PostgreSQL                     | Lưu trữ dữ liệu hệ thống (Schema-based)          |
| **Cache**    | Redis                          | Hỗ trợ lưu trữ JWT blacklist, caching OTP        |
| **Bảo mật**  | Spring Security + JWT          | Encode mật khẩu với BCrypt + Salt + Pepper       |

---

## Yêu cầu môi trường (Prerequisites)

Để chạy được dự án từ mã nguồn, máy tính của bạn cần cài đặt sẵn:

1. **JDK 25**: Bắt buộc dùng Java 25 (Có thể cài qua `winget install EclipseAdoptium.Temurin.25.JDK` trên Windows).
2. **Node.js 20+** & **npm 10+**: Để chạy Frontend.
3. **Docker & Docker Compose**: Để tự động giả lập database PostgreSQL và Redis thay vì cài đặt thủ công.
4. **Maven**: Đã được đính kèm sẵn trong thư mục backend (`mvnw.cmd`).

---

## Hướng dẫn cài đặt và chạy ứng dụng

### Bước 1: Khởi động Hạ tầng (Database & Cache)

Sử dụng Docker để khởi động nhanh PostgreSQL và Redis. Mở terminal tại thư mục gốc của dự án:

```bash
docker compose up -d
```

> **Lưu ý:** Lần đầu tiên chạy, PostgreSQL container sẽ tự động mount và chạy file `database/pccms_database_schema.sql` để tạo toàn bộ bảng và seed tài khoản Admin mặc định.

### Bước 2: Cấu hình Backend

File cấu hình mặc định là `backend/src/main/resources/application.yml`.
Nếu chưa có file này, hãy copy từ file example:

```bash
cp backend/src/main/resources/application.yaml.example backend/src/main/resources/application.yml
```

_Bạn có thể giữ nguyên cấu hình mặc định để chạy ở môi trường Local._

### Bước 3: Chạy Backend (Spring Boot)

Mở một terminal mới, trỏ vào thư mục `backend`:

```bash
cd backend
./mvnw.cmd spring-boot:run
```

- Backend sẽ khởi chạy tại: `http://localhost:8080/api`
- API Health Check: `http://localhost:8080/actuator/health`

### Bước 4: Chạy Frontend (React + Vite)

Mở một terminal khác, trỏ vào thư mục `frontend`:

```bash
cd frontend
npm install
npm run dev
```

- Frontend sẽ chạy tại địa chỉ: `http://localhost:5173` (hoặc cổng khác nếu 5173 đang bận, xem log terminal để lấy link chính xác).

---

## Thông tin tài khoản chạy thử (Demo Accounts)

Hệ thống cung cấp sẵn **01 tài khoản Admin cao nhất** sau khi khởi tạo Database theo secret của application.yaml.example. Bạn cần dùng tài khoản này đăng nhập vào hệ thống, sau đó vào mục Quản trị để cấp phát các tài khoản cho Bác sĩ, Lễ tân hoặc Chủ thú cưng.

| Vai trò (Role)       | Email             | Mật khẩu (Password) | Phân hệ (Dashboard) |
| -------------------- | ----------------- | ------------------- | ------------------- |
| **Admin (Quản trị)** | `admin@pccms.com` | `MyPassword123`     | `/admin`            |

### Hướng dẫn test các Role khác:

1. Đăng nhập bằng tài khoản `admin@pccms.com` bên trên.
2. Truy cập phân hệ Admin, tạo mới User và gán Role tương ứng (Ví dụ: `VETERINARIAN` cho Bác sĩ, `RECEPTIONIST` cho Lễ tân).
3. Đăng xuất và đăng nhập lại bằng tài khoản vừa tạo để trải nghiệm các Dashboard riêng biệt dành cho từng nghiệp vụ:
   - **Bác sĩ**: Xem hàng đợi, bắt đầu ca khám, chẩn đoán, kê đơn...
   - **Lễ tân**: Duyệt lịch, check-in, thu ngân...
   - **Chủ nuôi**: Đặt lịch trực tuyến, theo dõi bệnh án...
