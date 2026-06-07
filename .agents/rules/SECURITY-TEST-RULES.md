---
trigger: manual
---

# [SECURITY-TEST-RULES] MANDATORY SECURITY TESTING STANDARDS FOR PCCMS

> **PURPOSE (MỤC ĐÍCH)**
>
> - Security Testing nhằm xác minh hệ thống bảo vệ được 5 yếu tố cốt lõi: `Authentication` (Xác thực), `Authorization` (Phân quyền), `Confidentiality` (Tính bảo mật), `Integrity` (Tính toàn vẹn), và `Availability` (Tính khả dụng).
> - Security Testing là **BẮT BUỘC** đối với mọi chức năng có liên quan đến: Xác thực/Phân quyền, Dữ liệu cá nhân (Personal Data), Dữ liệu y tế (Medical Data), Dữ liệu thanh toán (Payment Data), và Các thao tác quản trị (Administrative Operations).
> - **LƯU Ý:** Security Test không được thay thế bằng Code Review.

---

## RULE 1 – SECURITY TEST DESIGN FIRST (THIẾT KẾ BẢO MẬT TRƯỚC)

Trước khi viết Security Test, AI Agent BẮT BUỘC phải xác định luồng sau:
`Assets` (Tài sản) → `Threats` (Mối đe dọa) → `Attack Surface` (Bề mặt tấn công) → `Security Controls` (Biện pháp bảo vệ) → `Security Test Cases`.

- **Cấm:** Không được sinh testcase ngẫu nhiên.
- Mọi Security Test phải truy vết được tới: Security Requirement, Security Control, và Threat Scenario.

---

## RULE 2 – AUTHENTICATION TESTING (KIỂM THỬ XÁC THỰC)

Mọi API yêu cầu đăng nhập phải kiểm tra các kịch bản sau:

- **Valid Authentication** (Valid JWT / Refresh Token) ➔ Kết quả: `200 OK`.
- **Missing Authentication** (Không gửi Token) ➔ Kết quả: `401 Unauthorized`.
- **Invalid Authentication** (JWT sai chữ ký/Bị giả mạo) ➔ Kết quả: `401 Unauthorized`.
- **Expired Authentication** (JWT hết hạn) ➔ Kết quả: `401 Unauthorized`.
- **Tampered Token** (Payload bị sửa đổi trái phép) ➔ Kết quả: `401 Unauthorized`.

---

## RULE 3 – AUTHORIZATION TESTING (KIỂM THỬ PHÂN QUYỀN)

Mọi API được bảo vệ bằng Role hoặc Permission phải kiểm tra toàn bộ Role Matrix (`ADMIN`, `VETERINARIAN`, `RECEPTIONIST`, `CUSTOMER`):

- **Authorized Access** (Người dùng có quyền hợp lệ) ➔ Kết quả: `Success (200/201)`.
- **Unauthorized Access** (Người dùng có token nhưng không đủ Role/Quyền) ➔ Kết quả: `403 Forbidden`.
- **Anonymous Access** (Người dùng chưa đăng nhập) ➔ Kết quả: `401 Unauthorized`.

---

## RULE 4 – IDOR TESTING (INSECURE DIRECT OBJECT REFERENCE)

IDOR là lỗ hổng nghiêm trọng bắt buộc phải kiểm tra trong kiến trúc đa người dùng.

- **Kịch bản:** User A cố tình truy cập resource của User B (Ví dụ: `GET /medical-records/{recordId_of_User_B}`).
- **Kết quả mong đợi:** `403 Forbidden` hoặc `404 Not Found`.
- **Cấm:** Tuyệt đối KHÔNG ĐƯỢC trả về dữ liệu của người khác.

---

## RULE 5 – INPUT VALIDATION TESTING (KIỂM THỬ DỮ LIỆU ĐẦU VÀO)

Không bao giờ tin tưởng dữ liệu từ Client. Mọi API nhận dữ liệu đầu vào phải kiểm tra:

- `Empty Input` (Dữ liệu rỗng)
- `Null Input` (Thiếu trường dữ liệu)
- `Invalid Format` (Sai định dạng email, sđt, UUID...)
- `Oversized Input` (Vượt quá độ dài cho phép)
- `Unexpected Input` (Ký tự đặc biệt không mong muốn)

---

## RULE 6 – SQL INJECTION TESTING

Mọi API có truy vấn dữ liệu (Search, Filter) phải kiểm tra các payload độc hại.

- **Ví dụ payload:** `'`, `''`, `OR 1=1`, `UNION SELECT`.
- **Kết quả mong đợi:** Không lộ dữ liệu trái phép, Không lỗi hệ thống (Crash/500), Không bypass được authentication.

---

## RULE 7 – XSS TESTING (CROSS-SITE SCRIPTING)

Mọi dữ liệu có thể hiển thị lại trên giao diện (Stored XSS / Reflected XSS) phải kiểm tra.

- **Ví dụ payload:** `<script>alert(1)</script>`, `<img src=x onerror=alert(1)>`.
- **Kết quả mong đợi:** Script KHÔNG ĐƯỢC thực thi. Dữ liệu phải được escape/sanitize trước khi lưu hoặc render.

---

## RULE 8 – CSRF TESTING (CROSS-SITE REQUEST FORGERY)

- **Nếu hệ thống sử dụng Cookie Authentication:** Phải kiểm tra Missing CSRF Token và Invalid CSRF Token ➔ Request phải bị từ chối (`403 Forbidden`).
- **Nếu sử dụng hoàn toàn JWT Bearer Token (Stateless):** Ghi nhận CSRF không áp dụng (Bỏ qua).

---

## RULE 9 – FILE UPLOAD SECURITY TESTING

Nếu hệ thống cho phép upload file (Ví dụ: Upload ảnh thú cưng, kết quả xét nghiệm):
Phải kiểm tra:

- `Invalid Extension` (Tải lên file `.php`, `.exe`, `.sh`).
- `Executable File` (File chứa mã độc).
- `Oversized File` (File vượt quá giới hạn MB).
- `Empty File` (File 0 byte).
- `MIME Type Mismatch` (Đổi đuôi file `.exe` thành `.jpg`).
- **Kết quả:** Không được lưu file nguy hiểm vào hệ thống.

---

## RULE 10 – SENSITIVE DATA EXPOSURE TESTING (LỘ DỮ LIỆU NHẠY CẢM)

Response trả về từ API BẮT BUỘC phải được kiểm tra. Không bao giờ được phép trả về:

- `Password` (Plain text)
- `Password Hash`
- `Refresh Token` (Ngoại trừ API login/refresh)
- `Internal Secret / Keys`
- `Stack Trace` hoặc `Internal Exception Detail`

---

## RULE 11 – ERROR HANDLING SECURITY TESTING

Khi có lỗi hệ thống xảy ra:

- **Cấm trả về:** Câu lệnh SQL (SQL Statement), Tên bảng (Table Name), Đường dẫn nội bộ (Internal Path), Stack Trace.
- **Phải trả về:** Standard Error Response (Ví dụ bọc trong `ApiResponse` với `code: 500` và thông báo lỗi chung chung an toàn).

---

## RULE 12 – RATE LIMITING TESTING

Để chống Brute-force và DoS. Đối với các luồng nhạy cảm: `Login`, `OTP`, `Password Reset`.

- **Phải kiểm tra:** Repeated Requests (Gửi liên tục hàng trăm request).
- **Kết quả mong đợi:** Rate Limit được áp dụng (`429 Too Many Requests`).

---

## RULE 13 – SESSION SECURITY TESTING

Nếu hệ thống có sử dụng Session (Hoặc Refresh Token Blacklist):

- Phải kiểm tra: Session Expiration (Hết hạn), Session Invalidation, và Logout Behaviour.
- **Kết quả:** Session cũ hoặc Token đã bị thu hồi/logout KHÔNG ĐƯỢC tiếp tục sử dụng.

---

## RULE 14 – AUDIT LOG TESTING (NHẬT KÝ KIỂM TOÁN)

Các hành động quan trọng PHẢI được ghi nhận.

- **Ví dụ:** `Login`, `Logout`, `Medical Record Update`, `Invoice Approval`, `Payment Processing`.
- **Phải xác minh:** Audit Log được tạo chính xác dưới DB (chứa thông tin Actor, Action, Target, Timestamp, IP).

---

## RULE 15 – SECURITY TEST DATA

- **Cấm:** TUYỆT ĐỐI KHÔNG sử dụng Production User, Production Token, hoặc Production Credential để chạy Security Test.
- **Bắt buộc:** Sử dụng Dedicated Security Test Accounts (Tài khoản được tạo riêng biệt cho môi trường Test).

---

## RULE 16 – AUTOMATED SECURITY TESTING (DEVSECOPS)

Mọi Release phải được thực thi:

- Static Security Testing (SAST).
- Secret Detection (Quét lộ Key/Token trên mã nguồn).
- Dependency Vulnerability Scan (Quét lỗ hổng thư viện).
- Dynamic Security Testing (DAST) bao gồm: Authentication Tests, Authorization Tests, Security Regression Tests.

---

## RULE 17 – OWASP COVERAGE (BẮT BUỘC RÀ SOÁT OWASP TOP 10)

AI Agent phải xem xét tối thiểu các rủi ro thuộc OWASP Top 10, bao gồm:

1. Broken Access Control
2. Cryptographic Failures
3. Injection
4. Insecure Design
5. Security Misconfiguration
6. Vulnerable and Outdated Components
7. Identification and Authentication Failures
8. Software and Data Integrity Failures
9. Security Logging and Monitoring Failures
10. Server-Side Request Forgery (SSRF)

---

## RULE 18 – SUCCESS CRITERIA (TIÊU CHÍ HOÀN THÀNH SECURITY TEST)

Một chức năng chỉ được xem là "Đạt yêu cầu bảo mật" khi:

- [ ] Authentication được kiểm thử.
- [ ] Authorization được kiểm thử (Bao quát toàn bộ Role Matrix).
- [ ] IDOR được kiểm thử triệt để.
- [ ] Input Validation được kiểm thử.
- [ ] SQL Injection được kiểm thử.
- [ ] XSS được kiểm thử.
- [ ] Sensitive Data Exposure được kiểm thử (Không lộ dữ liệu nhạy cảm).
- [ ] Error Handling được kiểm thử (Không lộ Stack Trace).
- [ ] Audit Logging được kiểm thử.
- [ ] OWASP Top 10 được rà soát.
- [ ] KHÔNG tồn tại lỗ hổng mức Critical hoặc High chưa được chấp thuận (Risk Accepted).
