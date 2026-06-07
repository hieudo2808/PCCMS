# Spec: IAM Account & Pet Administration Subsystem

## Objective
Xây dựng tính năng quản trị danh tính (Identity) và hồ sơ (Account & Pet Administration) cho PCCMS. Mục tiêu bao gồm việc quản lý vòng đời tài khoản (Registration, Verification, Lock, Disable), phân quyền dựa trên Role (RBAC), kiểm soát quyền sở hữu dữ liệu (Data Ownership - IDOR Prevention), và quản lý hồ sơ thú cưng (Pet Profiles) với logic nghiệp vụ chặt chẽ (Soft delete, Age validation, Auto-cancel appointments).

## Tech Stack
- Java 25, Spring Boot 4.0.5
- PostgreSQL (dùng Testcontainers cho Integration Test)
- MapStruct cho DTO mapping, Lombok.
- JUnit 5, Mockito, AssertJ (cho TDD).

## Commands
- Build: `./mvnw clean package -DskipTests`
- Test: `./mvnw test` (cho Unit Test) & `./mvnw verify` (cho Integration Test)
- Lint: `./mvnw checkstyle:check` (nếu có)
- Dev: `./mvnw spring-boot:run`

## Project Structure
```
backend/src/main/java/com/astral/express/pccms/
├── identity/       → Xử lý Auth, Token Revocation, RBAC, Roles.
├── user/           → Xử lý thông tin Users, Account Lifecycle (Lock/Disable).
├── pet/            → Xử lý Pet profiles (Species, Breeds, Pets).
backend/src/test/java/com/astral/express/pccms/
├── pet/            → Unit Test & Integration Test cho Pet.
└── user/           → Unit Test & Integration Test cho User.
backend/src/test/resources/testcases/
├── pet-age-validation.csv  → CSV test data cho kiểm thử tham số hóa.
```

## Code Style
- **Naming Conventions**: PascalCase cho Class, camelCase cho Biến/Hàm, UPPER_SNAKE_CASE cho Hằng số.
- **DTOs**: Bắt buộc sử dụng `record`.
- **Exception**: Dùng `BusinessException(ErrorCode)`.
- **Controllers**: Luôn bọc trong `ApiResponse<T>` hoặc `PageResponse<T>`.

## Testing Strategy
- **Test-Driven Development (TDD)**: Viết test trước khi code nghiệp vụ.
- **Test Design**: Tuân thủ Black-box (EP, BVA) và White-box (100% Branch Coverage cho Service layer).
- **CSV Driven**: Parameterized Tests bắt buộc dùng CSV mapping với Rule ID.
- **Integration Test**: Tách biệt, dùng Testcontainers, test DB mapping và Soft Delete.
- **Security Test**: Phải cover Authorization, Authentication và IDOR.

## Boundaries
- **Always do**: Kiểm tra Ownership (`pet.getOwnerId().equals(currentUserId)`) đối với ROLE_CUSTOMER. Dùng Soft Delete (`isActive = false`) thay vì xóa cứng. Thu hồi Refresh Token khi khóa/vô hiệu hóa tài khoản.
- **Ask first**: Chỉnh sửa Schema Database, Cập nhật dependencies mới.
- **Never do**: Trả về dữ liệu nhạy cảm (password_hash) trong Response DTO. Để lộ IDOR. Thực hiện xóa vật lý (Hard delete).

## Success Criteria
- [ ] Role `ADMIN` có thể khóa/vô hiệu hóa user, và hệ thống tự động thu hồi Refresh Token.
- [ ] API cấp quyền và thao tác quản trị được bảo vệ bởi `@PreAuthorize("hasRole('ADMIN')")`.
- [ ] Role `CUSTOMER` bị chặn truy cập (403 Forbidden) nếu thao tác trên thú cưng không thuộc sở hữu.
- [ ] Logic kiểm tra tuổi thú cưng (`birth_date` hoặc `estimated_age_months`) hoạt động chính xác theo TDD.
- [ ] Khi thú cưng bị đánh dấu Inactive, hệ thống tự động Cancel các lịch hẹn Pending/Reserved.
- [ ] Pet chỉ được Soft Delete.
- [ ] Coverage tầng Service đạt trên 85% và 100% Branch Coverage.
- [ ] Security Test pass 100% các case 401/403/IDOR.

## Open Questions
- Với chức năng "tự động Cancel các lịch hẹn Pending/Reserved", hiện tại chúng ta chưa có Module `Appointment` hay `Grooming`/`Boarding`. Tôi có nên tạo sẵn interface/event (ví dụ `PetDeactivatedEvent`) để module khác lắng nghe thay vì gọi trực tiếp sang chưa? (Khuyến nghị: Dùng Event-Driven bằng `ApplicationEventPublisher`).
- `weight_kg` validation: Nếu người dùng nhập sai, có cho phép null không hay bắt buộc? Đặc tả ghi "`weight_kg` must be strictly > 0", vậy là có bắt buộc phải nhập không?
