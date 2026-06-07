# [IAM-ACCOUNT-GUIDE] ĐẶC TẢ NGHIỆP VỤ ĐỊNH DANH VÀ QUẢN TRỊ HỒ SƠ

> **MỤC ĐÍCH TỐI THƯỢNG:** Tài liệu này đặc tả luồng nghiệp vụ cốt lõi về Identity & Access Management (IAM), Quản lý Vai trò (Role Management) và Quản trị Hồ sơ (Account & Pet Administration). AI Agent **BẮT BUỘC** phải tuân thủ các quy định về kiểm soát truy cập và vòng đời tài khoản dưới đây khi sinh code cho module này.

---

## RULE 1 – VÒNG ĐỜI TÀI KHOẢN (USER ACCOUNT LIFECYCLE)

Tài khoản người dùng (Bảng `users`) tuân thủ nghiêm ngặt trạng thái dựa trên `user_status_enum`.

- **Khởi tạo (Registration):** Người dùng tự đăng ký (Customer) mặc định trạng thái là `UNVERIFIED`.
- **Kích hoạt (Verification):** Hệ thống chỉ chuyển sang `ACTIVE` khi xác thực thành công Email hoặc Số điện thoại.
- **Khóa & Vô hiệu hóa (Lock/Disable):**
  - Chỉ `ADMIN` mới có quyền chuyển trạng thái tài khoản sang `LOCKED` (Khóa tạm thời) hoặc `DISABLED` (Vô hiệu hóa vĩnh viễn).
  - Khi tài khoản bị chuyển sang `LOCKED` hoặc `DISABLED`, hệ thống BẮT BUỘC phải thực hiện thao tác **Thu hồi Token (Token Revocation)**: Xóa hoặc đưa vào blacklist toàn bộ Refresh Token của user đó ngay lập tức.
- **Không xóa vật lý (No Hard Delete):** Khi có yêu cầu xóa tài khoản, chỉ được phép đổi trạng thái sang `DISABLED` và cập nhật `updated_at`. Tuyệt đối không gọi `repository.delete()`.

---

## RULE 2 – MA TRẬN PHÂN QUYỀN (ROLE & PERMISSION MATRIX)

AI Agent phải áp dụng `@PreAuthorize` dựa trên ma trận quyền sau:

1. **`ROLE_ADMIN` (Quản trị viên):**
   - Có toàn quyền (Full Access) trên hệ thống.
   - Là role duy nhất được phép: Tạo tài khoản nhân viên (Staff/Vet), Phân quyền, Xem báo cáo doanh thu tổng, và Khóa tài khoản người dùng khác.
2. **`ROLE_VETERINARIAN` (Bác sĩ thú y):**
   - Được phép: Truy cập tất cả `medical_records`, `prescriptions`, `appointments` (thuộc danh mục MEDICAL).
   - KHÔNG được phép: Cập nhật hóa đơn, thao tác tính tiền, hoặc can thiệp vào dịch vụ Grooming/Boarding.
3. **`ROLE_RECEPTIONIST` (Lễ tân/Nhân viên):**
   - Được phép: Quản lý lịch hẹn (`appointments`), Đăng ký dịch vụ lưu trú/làm đẹp (`grooming_tickets`, `boarding_bookings`), Quản lý hóa đơn (`invoices`).
   - KHÔNG được phép: Tạo, sửa, hoặc chốt bệnh án (`medical_records`), Kê đơn thuốc.
4. **`ROLE_CUSTOMER` (Chủ nuôi):**
   - Được phép: Truy cập hệ thống (Web/App dành cho khách hàng).
   - **Bị giới hạn tuyệt đối bởi Data Ownership (Xem Rule 3).**

---

## RULE 3 – KIỂM SOÁT QUYỀN SỞ HỮU DỮ LIỆU (DATA OWNERSHIP & IDOR PREVENTION)

Đây là luật quan trọng nhất để chống lỗ hổng IDOR. Đối với `ROLE_CUSTOMER`, hệ thống BẮT BUỘC phải kiểm tra quyền sở hữu đối với mọi Entity.

### 3.1. Đối với Hồ sơ Thú cưng (Pets)

- **Truy vấn (GET):** Khách hàng chỉ lấy được danh sách `pets` có `owner_id` khớp với `userId` lấy từ JWT Token.
- **Cập nhật/Xóa (PUT/DELETE):** Trước khi thao tác, Agent phải thêm guard clause:
  ```java
  if (!pet.getOwnerId().equals(currentUserId)) {
      throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
  }
  ```

### 3.2. Đối với Hồ sơ Y tế & Lịch hẹn (Medical Records / Appointments)

- Bệnh án (`medical_records`) kết nối với thú cưng (`pet_id`). Chủ nuôi chỉ được XEM bệnh án của thú cưng thuộc sở hữu của mình. CẤM chủ nuôi thao tác CẬP NHẬT hoặc XÓA bệnh án/lịch hẹn đã được trung tâm xác nhận.

---

## RULE 4 – QUẢN TRỊ HỒ SƠ THÚ CƯNG (PET PROFILE ADMINISTRATION)

- **Validation Tuổi (Age Validation):** Khi tạo/sửa thông tin thú cưng, hệ thống BẮT BUỘC phải kiểm tra logic: Phải nhập `birth_date` HOẶC `estimated_age_months`. Nếu cả hai đều null, ném lỗi `BusinessException(ErrorCode.ERR_PET_INVALID_AGE_DATA)`.
- **Logic Báo Mất / Tử Vong:** - Thay vì xóa hồ sơ, chủ nuôi hoặc nhân viên có thể đánh dấu thú cưng không còn hoạt động (Ví dụ: Chuyển cờ hoặc đổi status).
  - Khi một thú cưng bị đánh dấu Inactive, hệ thống BẮT BUỘC phải quét và tự động `CANCEL` toàn bộ các lịch hẹn (`appointments`, `grooming_tickets`, `boarding_bookings`) đang ở trạng thái chờ (`PENDING`, `RESERVED`) của thú cưng đó trong tương lai.

---

## RULE 5 – QUẢN LÝ PHIÊN ĐĂNG NHẬP (SESSION & JWT MANAGEMENT)

- **Token Payload:** Access Token (JWT) chỉ được phép chứa: `sub` (userId), `email`, và mảng `roles`. TUYỆT ĐỐI không nhúng các thông tin PII (Personally Identifiable Information) khác như tên thật, số điện thoại, địa chỉ vào Token.
- **Refresh Token Lifecycle:** Refresh Token phải được lưu dưới Database với cột `expires_at` và `revoked_at`. API `POST /api/v1/auth/refresh` BẮT BUỘC phải check:
  1. Refresh Token còn hạn (`expires_at > NOW()`).
  2. Refresh Token chưa bị thu hồi (`revoked_at IS NULL`).
  3. Tài khoản của User đó vẫn đang `ACTIVE` (Nếu user bị Admin khóa, Refresh Token phải bị từ chối ngay lập tức).

---

## LUẬT THỰC THI (EXECUTION) DÀNH CHO AI AGENT

Khi sinh code cho Module Identity & Account, AI Agent phải đối chiếu Checklist sau:

- [ ] API lấy/cập nhật thông tin Pet đã check `owner_id == currentUserId` chưa?
- [ ] Mật khẩu (Password) đã được băm (hash) bằng BCrypt trước khi lưu chưa?
- [ ] API cấp quyền/Khóa tài khoản đã gắn `@PreAuthorize("hasRole('ADMIN')")` chưa?
- [ ] Logic lấy thông tin `currentUser` BẮT BUỘC đọc từ `SecurityContextHolder` thay vì tin tưởng vào Request Body?
