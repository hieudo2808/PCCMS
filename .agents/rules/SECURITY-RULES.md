---
trigger: manual
---

# [SECURITY-RULES] QUY CHUẨN BẢO MẬT & PHÂN QUYỀN HỆ THỐNG (PCCMS)

> **MỤC ĐÍCH:** Tài liệu này quy định các tiêu chuẩn bảo mật mã nguồn mở rộng. Bất kỳ mã nguồn nào do AI Agent sinh ra (đặc biệt là API, Service và Security Config) **BẮT BUỘC** phải tuân thủ nghiêm ngặt các quy tắc xác thực (Authentication), phân quyền (Authorization) và kiểm soát quyền sở hữu dữ liệu (Data Ownership) dưới đây.

---

## 1. QUY CHUẨN XÁC THỰC (AUTHENTICATION - JWT)

Hệ thống sử dụng **Stateless Authentication** thông qua JSON Web Token (JWT).

- **Cấu trúc Token:** Dữ liệu Payload (Claims) bên trong JWT chỉ được phép chứa: `accountId`, `email/phoneNumber`, và `role`. **TUYỆT ĐỐI KHÔNG** lưu mật khẩu, thông tin cá nhân (Profile) hay dữ liệu nhạy cảm khác vào JWT.
- **Xử lý ở Controller:** Mọi API (ngoại trừ các endpoint public như `/api/v1/auth/login` hoặc `/api/v1/auth/register`) đều BẮT BUỘC phải yêu cầu header `Authorization: Bearer <token>`.
- **Cấu hình Spring Security:** Agent bắt buộc sử dụng `SecurityFilterChain` với Lambda DSL (chuẩn của Spring Boot 3.x/4.x). Phải cấu hình `sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`. Cấm sử dụng Session/Cookie truyền thống.

---

## 2. QUY CHUẨN PHÂN QUYỀN (AUTHORIZATION - RBAC)

Hệ thống quản lý truy cập dựa trên vai trò (Role-Based Access Control). Các vai trò (Roles) hợp lệ bao gồm: `ADMIN`, `VETERINARIAN`, `RECEPTIONIST`, `CUSTOMER`.

- **Kiểm soát ở Controller (Method Security):** - Mọi API cần bảo vệ BẮT BUỘC phải gắn annotation `@PreAuthorize`.
  - _Ví dụ:_ `@PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")`.
- **Nguyên tắc "Đóng mặc định" (Default Deny):** Agent phải giả định rằng mọi API đều bị khóa. Chỉ mở khóa cho đúng Role được yêu cầu trong prompt hoặc SRS.

---

## 3. KIỂM SOÁT QUYỀN SỞ HỮU DỮ LIỆU (DATA OWNERSHIP VALIDATION) - ⚠️ QUAN TRỌNG NHẤT

Đây là lỗi bảo mật phổ biến nhất. Agent **BẮT BUỘC** phải cài cắm logic kiểm tra chủ sở hữu (Ownership) ở tầng Service đối với vai trò `CUSTOMER` (Chủ nuôi).

- **Luật:** Một `CUSTOMER` chỉ được phép XEM (Read), SỬA (Update) hoặc XÓA MỀM (Delete) dữ liệu (Thú cưng, Lịch hẹn, Bệnh án) thuộc về chính họ.
- **Hành động của Agent:** Trong các hàm xử lý ở Service, Agent phải trích xuất `accountId` của người đang đăng nhập (từ `SecurityContextHolder`) và so sánh với `ownerId` của Entity lấy từ Database.
- **Mẫu Code Bắt Buộc (Guard Clause):**

  ```java
  public PetProfileResponse getPetDetails(UUID petId, UUID currentUserId) {
      Pet pet = petRepository.findById(petId)
          .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

      // BẮT BUỘC PHẢI CÓ BLOCK NÀY NẾU ROLE LÀ CUSTOMER
      if (!pet.getOwnerId().equals(currentUserId) && !securityUtil.isAdminOrStaff()) {
          log.warn("Security Alert: User {} attempted to access Pet {} belonging to User {}",
                    currentUserId, petId, pet.getOwnerId());
          throw new BusinessException(ErrorCode.FORBIDDEN_ACCESS);
      }

      return mapper.toResponse(pet);
  }
  ```

```

---

## 4. XỬ LÝ MẬT KHẨU VÀ MÃ HÓA (CRYPTOGRAPHY)

* **Cấm Plaintext:** Mọi mật khẩu người dùng **TUYỆT ĐỐI KHÔNG ĐƯỢC** lưu dưới dạng plain text.
* **Công cụ Mã hóa:** Bắt buộc sử dụng `BCryptPasswordEncoder` (hoặc `Argon2PasswordEncoder`) được cung cấp bởi Spring Security.
* **Logging:** **CẤM** in mật khẩu, JWT token, mã OTP hoặc các thông tin thẻ tín dụng (nếu có) ra Console hay file log hệ thống (Log masking).
* *Sai:* `log.info("User logged in with password: " + request.getPassword());`
* *Đúng:* `log.info("User {} successfully logged in", request.getUsername());`



---

## 5. BẢO MẬT GIAO TIẾP VÀ MẠNG (NETWORK SECURITY)

* **CORS (Cross-Origin Resource Sharing):** Phải được cấu hình tường minh trong Spring Security để chỉ cho phép các domain của Frontend (React/Vite) đã được định nghĩa trong `application.yml` truy cập. Không dùng `.allowedOrigins("*")` trong môi trường Production.
* **CSRF (Cross-Site Request Forgery):** Vì hệ thống dùng API RESTful và JWT (Stateless), CSRF phải được tắt rõ ràng: `csrf(AbstractHttpConfigurer::disable)`.
* **SQL Injection:** Mọi thao tác Database phải dùng Spring Data JPA, Hibernate hoặc Named Parameters. **TUYỆT ĐỐI KHÔNG** nối chuỗi (String concatenation) khi viết câu lệnh `@Query`.
* *Sai:* `@Query("SELECT u FROM User u WHERE u.email = '" + email + "'")`
* *Đúng:* `@Query("SELECT u FROM User u WHERE u.email = :email")`



---

## 6. NHẬT KÝ KIỂM TOÁN (AUDIT TRAIL & LOGGING)

Theo yêu cầu hệ thống Y tế (Medical Care Subsystem) và Quản trị viên (Admin Account Service):

* Mọi thao tác nhạy cảm (Tạo bác sĩ mới, Xóa/Khóa tài khoản, Thay đổi bệnh án đã chốt, Cập nhật kho thuốc) **BẮT BUỘC** phải được ghi log với format: `[ACTION] - [USER_ID] - [TARGET_ENTITY_ID] - [TIMESTAMP]`.
* Việc ghi log này nên được thực hiện thông qua Spring AOP (Aspect-Oriented Programming) hoặc gọi `AuditLoggerService` để không làm bẩn code của logic nghiệp vụ (Business Logic).
* Việc ghi

> **XÁC NHẬN CỦA AGENT:** Bằng việc nạp file này, Agent cam kết mọi REST API sinh ra đều được bảo vệ bởi `@PreAuthorize`, mọi mật khẩu đều được Hash, và mọi truy xuất dữ liệu cá nhân đều phải qua bước kiểm tra Ownership.
```
