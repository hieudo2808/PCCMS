# Backend Architecture Overview

## Mục tiêu
Dự án sử dụng hình mẫu kiến trúc **Modular Monolith** kết hợp với **Layered Architecture**. Điều này giúp ứng dụng dễ quản lý hơn, giảm thiểu chi phí DevOps (như trong Microservices) nhưng vẫn giữ được sự phân tách rõ rệt giữa các mô-đun nghiệp vụ, tạo tiền đề nếu sau này mở rộng thành Microservices thực thụ.

## Công nghệ & Stack
- **Ngôn ngữ**: Java 25
- **Framework Chính**: Spring Boot 4.0.5
- **Database**: PostgreSQL
- **ORM**: Spring Data JPA
- **Caching / Rate Limiting**: Redis (Upstash)
- **Security**: Spring Security + JWT Authentication + Peppered BCrypt
- **Mappers**: MapStruct
- **Email**: Spring Mail
- **Dependencies Control**: Maven

## Cấu trúc tổng quan
Source code backend nằm bên trong thư mục `src/main/java/com/astral/express/pccms/`.
Được chia thành 4 phân hệ chính:

### 1. `common`
Chứa các thành phần được dùng chung (shared) xuyên suốt hệ thống.
- **DTO**: Cấu trúc trả về chuẩn `ApiResponse`.
- **Exception**: Cơ chế quản lý lỗi cục bộ, `ErrorCode`, `AppException` và `GlobalExceptionHandler` chặn bắt toàn bộ Exception.
- **Config**: Các config dùng chung như `ApiConfig` định tuyến `/api`, `RedisConfig`.
- **Helper**: Chứa các utility đặc thù, ví dụ `PasswordGenerator`.

### 2. `identity`
Phụ trách các luồng định danh, cấu hình bảo mật.
- **Controller**: `AuthController` mở các API liên quan tới `register`, `login`, `logout` và `refresh` token. 
- Mọi token được phát hành là JWT. Cơ chế *Refresh Token* có lưu lại ở Database.
- **Security**: Nơi chứa toàn bộ cấu trúc bảo vệ ứng dụng với `JwtAuthenticationFilter`, `RateLimitFilter` (để chống spam calls trên Redis), mã hóa mật khẩu `PepperBCryptEncoder`.
- **Token Blacklist**: Dịch vụ `TokenBlacklistService` giao tiếp với Redis để triệt tiêu các JWT khi có nhu cầu (logout đột ngột, đổi pass, v.v.).

### 3. `user`
Tập trung vào quản lý người dùng thực thụ bao gồm profiles, danh sách, quyền hạn (Roles/Permissions).
- **Entities**: Lõi trung tâm là bảng `Users`, `Roles`, `Permission`.
- **Logic**: Quản trị `UserService` phục vụ các API từ `UserController` để cho phép `Admin` quản trị danh sách người dùng, hoặc bản thân `User` tự cập nhật *My Profile*, *Change Password*.

### 4. `notification`
- Thực thi công việc liên quan thông báo. Hiện tại có chứa lớp `EmailService` thực thi logic gửi mail (Sử dụng SMTP Gmail) không đồng bộ `@Async` mỗi khi có các event (như *Tài khoản đã được tạo*).

## Quan hệ giữa các Modules (Layering)
Để đảm bảo Modular Monolith:
- **Nguyên tắc Layer**: Bên trong mỗi module, flow HTTP đi theo hướng: `Controller -> Service -> Repository -> Entity`. Không rẽ nhánh lộn xộn. Dữ liệu trung chuyển ra ngoài thông qua ranh giới `DTO` & `Mapper` (Mô hình DTO - Data Transfer Object).
- **Gọi chéo Module**: Module này gọi logic từ Module kia thông qua việc autowire *Service* hoặc import Entity một cách giới hạn. Ví dụ AuthService bên trong `identity` thao tác với UserRepository thuộc module `user`. (Thay vì gọi qua REST API hay message broker như Microservices cũ).
- **Lưu ý vòng lặp**: Tránh việc `user` gọi lại `identity` trong khi `identity` cũng phụ thuộc vào `user`. Để giải quyết, `SecurityHelper` trong `identity` có thể được chích vào, nhưng không mang nặng entity mapping chằng chéo.

## Hệ thống CI/CD & Deploy (Hiện tại)
- Chạy chung 1 cổng `8080`.
- Application config được tập trung duy nhất ở file `application.yml` gồm config về Database, Redis, Mail Server, JWT Secret & Expiration, và Pepper config cho mã hóa mật khẩu.
- Database đang sử dụng cấu hình PostgreSQL JDBC: `jdbc:postgresql://localhost:17288/pccms`.

## Cấu trúc File / Thư mục chính
```text
backend/src/main/java/com/astral/express/pccms/
├── PccmsApplication.java        (Main Boot Class)
├── common/             
│   ├── config/                  (VD: ApiConfig.java, RedisConfig.java)
│   ├── dto/                     (VD: ApiResponse.java)
│   ├── exception/               (VD: AppException.java, GlobalExceptionHandler.java)
│   └── helper/                  (VD: PasswordGenerator.java)
├── identity/          
│   ├── config/                  (VD: SecurityConfig.java, TokenCleanupScheduler.java)
│   ├── controller/              (VD: AuthController.java)
│   ├── dto/...                  
│   ├── entity/                  (VD: RefreshToken.java)
│   ├── repository/              (VD: RefreshTokenRepository.java)
│   ├── security/                (VD: JwtUtil.java, RateLimitFilter.java, JwtAuthenticationFilter.java, ...)
│   └── service/                 (VD: AuthService.java, CustomUserDetailsService.java, ...)
├── user/               
│   ├── controller/              (VD: UserController.java)
│   ├── dto/...                  
│   ├── entity/                  (VD: Users.java, Roles.java, Permission.java)
│   ├── mapper/                  (VD: UserMapper.java)
│   ├── repository/              (VD: UserRepository.java, RoleRepository.java)
│   └── service/                 (VD: UserService.java)
└── notification/       
    └── service/                 (VD: EmailService.java)
```
