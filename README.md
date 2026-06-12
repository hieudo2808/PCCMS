# PCCMS - Pet Care Center Management System

Hệ thống quản lý trung tâm chăm sóc thú cưng.

| Thành phần | Công nghệ |
|------------|-----------|
| Frontend | React 19 + Vite 8 + TypeScript |
| Backend | Java 25 + Spring Boot 4.0.5 (Modular Monolith, Layer Architecture) |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| Thiết kế | Astah UML |

## Yêu cầu môi trường

- **JDK 25** (bắt buộc cho backend — `pom.xml` dùng `java.version=25`)
- **Node.js** 20+ và **npm** 10+
- **Docker Desktop** (PostgreSQL + Redis qua `docker compose`)
- **Maven** (đã có sẵn `backend/mvnw.cmd`)

## Cài đặt nhanh (Windows)

```powershell
# Từ thư mục gốc dự án
.\scripts\setup.ps1
```

Script sẽ: khởi động Docker (Postgres + Redis), `npm install` frontend, tạo `.env` / `application.yml` nếu thiếu, và resolve Maven dependencies.

### JDK 25

Dự án bắt buộc JDK 25. Cài qua winget:

```powershell
winget install EclipseAdoptium.Temurin.25.JDK
```

Trỏ `JAVA_HOME` sang JDK 25 (mỗi terminal mới, hoặc đặt trong System Environment Variables):

```powershell
.\scripts\use-jdk25.ps1
java -version   # phải hiển thị version 25
```

Ví dụ đường dẫn cố định: `C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot`

## Chạy từng bước

### 1. Hạ tầng (PostgreSQL + Redis)

```powershell
docker compose up -d
```

Lần đầu khởi động, Postgres tự chạy script `database/pccms_database_schema.sql` (schema + seed).

| Dịch vụ | Host | User / Password |
|---------|------|-----------------|
| PostgreSQL | `localhost:5432` | `pccms` / `pccms` |
| Redis | `localhost:6379` | password: `redis-password` |

### 2. Cấu hình backend

File cấu hình local (gitignored): `backend/src/main/resources/application.yml`

Nếu chưa có, copy từ mẫu:

```powershell
Copy-Item backend\src\main\resources\application.yaml.example backend\src\main\resources\application.yml
```

Chỉnh `jwt.secret`, `security.pepper`, và SMTP khi triển khai thật.

### 3. Backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080` (REST controllers có prefix `/api` qua `ApiConfig`).

Health: `http://localhost:8080/actuator/health`

### 4. Frontend

```powershell
cd frontend
Copy-Item .env.example .env   # nếu chưa có
npm install
npm run dev
```

UI: `http://localhost:5173`

Biến môi trường:

```env
VITE_API_BASE_URL=http://localhost:8080/api
```

### Cloudflare R2 media storage

Backend upload file/media uses Cloudflare R2 through the S3-compatible API. Create an R2 bucket, attach a public custom domain, then set:

```env
R2_ACCOUNT_ID=your-account-id
R2_ACCESS_KEY_ID=your-r2-access-key
R2_SECRET_ACCESS_KEY=your-r2-secret-key
R2_BUCKET=pccms-media
R2_PUBLIC_BASE_URL=https://media.example.com
R2_CACHE_CONTROL=public,max-age=31536000,immutable
```

The backend stores the public URL in `file_assets.stored_key`, so existing frontend media rendering continues to use `media.url` directly. Keep bucket write access private to backend credentials; public read should go through the custom domain.

## Lệnh hữu ích

| Mục đích | Lệnh |
|----------|------|
| Build frontend | `cd frontend && npm run build` |
| Test frontend | `cd frontend && npm test` |
| Compile backend | `cd backend && .\mvnw.cmd compile` |
| Test backend | `cd backend && .\mvnw.cmd test` |
| Lint frontend | `cd frontend && npm run lint` |

## Cấu trúc thư mục

```
PCCMS/
├── backend/          # Spring Boot modules (identity, user, pet, medicalrecord, ...)
├── frontend/         # React + Vite
├── database/         # PostgreSQL schema SQL
├── docker-compose.yml
└── scripts/setup.ps1
```

## Tài khoản demo (local)

Mật khẩu trong DB phải khớp `security.pepper` của backend (`PepperBCryptEncoder`). Sau khi init schema hoặc khi đăng nhập báo **sai mật khẩu**, chạy:

```powershell
Get-Content database\seeds\reset_demo_passwords.sql -Encoding UTF8 -Raw | docker exec -i pccms-postgres psql -U pccms -d pccms
```

| Vai trò | Email | Mật khẩu | Sau đăng nhập |
|---------|-------|----------|---------------|
| Chủ nuôi | `owner@pccms.vn` | `owner123` | `/owner` — đặt lịch, xem lịch hẹn |
| Chủ nuôi (có 3 thú cương) | `hoangvanthang.work@gmail.com` | `owner123` | `/owner` |
| Nhân viên lễ tân | `staff.le@pccms.vn` | `staff123` | `/staff/appointments` — tiếp nhận, walk-in |
| Bác sĩ | `vet.an@pccms.vn` | `vet123` | `/veterinarian/queue` |
| Quản trị | `admin@pccms.vn` | `admin123` | `/admin` |

**Duyệt lịch khám:** đăng nhập STAFF → **Tiếp nhận lịch hẹn** → lọc *Chờ tiếp nhận* → **Tiếp nhận** / **Hủy**.

**Duyệt spa:** STAFF → **Bảng spa (grooming)** → chuyển trạng thái *Chờ xác nhận* → *Đang phục vụ* → *Hoàn thành*.

## Ghi chú

- `spring.jpa.hibernate.ddl-auto: validate` — schema phải khớp với DB (dùng file SQL trong `database/`).
- Mail mặc định trỏ `localhost:1025` trong `application.yml` local; đổi trong file cấu hình nếu dùng SMTP thật.
- Commit message: [Conventional Commits](https://www.conventionalcommits.org/).
