# [COMMON-CORE-GUIDE] ĐẶC TẢ CORE ARCHITECTURE & COMMON RESPONSES

> **MỤC ĐÍCH TỐI THƯỢNG:** Tài liệu này thiết lập "Bộ luật nền tảng" (Core Infrastructure) cho toàn bộ hệ thống PCCMS. AI Agent khi viết mã tại tầng Controller, xử lý phân trang, hoặc ném ngoại lệ ở tầng Service **BẮT BUỘC** phải tuân thủ nghiêm ngặt các cấu trúc Response và Exception định nghĩa tại đây. Tuyệt đối không được sáng chế ra các format trả về khác.

---

## RULE 1 – API RESPONSE WRAPPER (CHUẨN HÓA KẾT QUẢ TRẢ VỀ)

Mọi REST API khi thành công (`HTTP Status 200/201`) BẮT BUỘC phải bọc dữ liệu trong class `com.pccms.common.response.ApiResponse<T>`.

- **CẤM:** Không được phép trả về trực tiếp DTO, String, hay Entity từ Controller.
- **Cấu trúc JSON bắt buộc:**

```json
{
  "success": true,
  "code": 200,
  "message": "Thao tác thành công",
  "data": { ... } // Payload thực tế (DTO) nằm ở đây
}
```

- **Hành vi của Agent:**
  - `return ApiResponse.success(dto);` (Mặc định code 200)
  - `return ApiResponse.created(dto);` (Mặc định code 201 cho POST)
  - `return ApiResponse.success(null, "Xóa thành công");` (Cho các API void)

---

## RULE 2 – PAGINATION STANDARDS (CHUẨN HÓA PHÂN TRANG)

Hệ thống KHÔNG BAO GIỜ query toàn bộ dữ liệu (VD: Không dùng `findAll()`). Mọi API danh sách phải dùng phân trang và bọc bằng `com.pccms.common.response.PageResponse<T>`.

- **Quy ước tham số đầu vào (Request):**
  - Mọi API List phải nhận params: `page` (int, default = 1), `size` (int, default = 20), `sort` (String, default = "createdAt:desc").
  - **Giới hạn an toàn:** Tầng Service phải check `size`. Tối đa `size = 100` để chống DoS. Nếu client gửi `size = 1000`, tự động ép về `100`.
- **Ánh xạ Page (Spring Data):** AI Agent phải tự động map đối tượng `org.springframework.data.domain.Page<T>` của Spring Data JPA sang `PageResponse<T>` trước khi trả về.
- **Cấu trúc JSON bắt buộc:**

```json
{
  "success": true,
  "code": 200,
  "message": "Thành công",
  "data": {
    "content": [ ... ],       // Mảng dữ liệu
    "pageNumber": 1,          // Trang hiện tại (Bắt đầu từ 1, không phải 0)
    "pageSize": 20,           // Kích thước trang
    "totalElements": 150,     // Tổng số bản ghi
    "totalPages": 8,          // Tổng số trang
    "isFirst": true,          // Có phải trang đầu không?
    "isLast": false           // Có phải trang cuối không?
  }
}
```

---

## RULE 3 – EXCEPTION HANDLING ARCHITECTURE (KIẾN TRÚC XỬ LÝ LỖI)

AI Agent BẮT BUỘC áp dụng cơ chế xử lý lỗi tập trung qua `@RestControllerAdvice` (`GlobalExceptionHandler.java`).

### 3.1. Business Exception (Ngoại lệ Nghiệp vụ)

- Tầng `Service` khi gặp lỗi nghiệp vụ (Ví dụ: Không đủ tồn kho, Thú cưng đã bị xóa) BẮT BUỘC ném ra `com.pccms.common.exception.BusinessException`.
- **KHÔNG ĐƯỢC PHÉP:** Ném `RuntimeException`, `IllegalArgumentException` hay `Exception` chung chung.
- **Cú pháp bắt buộc:** `throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);`

### 3.2. Error Response Wrapper (Chuẩn hóa JSON Lỗi)

Mọi Exception do hệ thống ném ra sẽ được `GlobalExceptionHandler` chặn lại và bọc thành định dạng:

```json
{
  "success": false,
  "code": 400,
  "message": "Số lượng thuốc trong kho không đủ.",
  "errorCode": "ERR_BIZ_INSUFFICIENT_STOCK", // Phải có mã lỗi text để Frontend switch/case
  "errors": null
}
```

### 3.3. Validation Error (Lỗi đầu vào)

- Khi dùng `@Valid` ở Controller và DTO bị lỗi, `MethodArgumentNotValidException` sẽ bị ném ra.
- Agent phải đảm bảo `GlobalExceptionHandler` gom tất cả các lỗi của các field vào mảng `errors`.

```json
{
  "success": false,
  "code": 400,
  "message": "Dữ liệu đầu vào không hợp lệ",
  "errorCode": "ERR_VALIDATION_FAILED",
  "errors": {
    "email": "Định dạng email không đúng",
    "phoneNumber": "Số điện thoại không được để trống"
  }
}
```

---

## RULE 4 – ERROR CODE DICTIONARY (TỪ ĐIỂN MÃ LỖI)

Mọi mã lỗi phải được định nghĩa tập trung tại Enum `com.pccms.common.exception.ErrorCode`. AI Agent **CẤM** được hardcode chuỗi thông báo lỗi (Magic Strings) trực tiếp trong Service. Khi cần mã lỗi mới, phải khai báo thêm vào Enum này.

**Cấu trúc chuẩn của Enum ErrorCode:**

- `errorCode` (String): Mã lỗi text (Ví dụ: `ERR_USER_NOT_FOUND`).
- `httpStatus` (int): Mã HTTP trả về (Ví dụ: `404`).
- `message` (String): Thông báo tiếng Việt cho người dùng (Ví dụ: `Không tìm thấy thông tin người dùng`).

**Danh sách Nhóm Lỗi Bắt buộc (Must-Have Categories):**

1. **Lỗi Hệ Thống (System/HTTP):**
   - `ERR_500_INTERNAL_SERVER`: Lỗi máy chủ cục bộ.
   - `ERR_400_BAD_REQUEST`: Request không hợp lệ.
2. **Lỗi Xác Thực / Phân Quyền (IAM):**
   - `ERR_401_UNAUTHORIZED`: Chưa đăng nhập hoặc token hết hạn.
   - `ERR_403_FORBIDDEN`: Không có quyền truy cập tài nguyên này.
3. **Lỗi Nghiệp vụ Y tế (Medical Care):**
   - `ERR_MED_001_RECORD_LOCKED`: Bệnh án đã chốt, không thể chỉnh sửa.
   - `ERR_MED_002_INSUFFICIENT_STOCK`: Không đủ thuốc trong kho.
   - `ERR_MED_003_INVALID_VITALS`: Chỉ số sinh hiệu không hợp lệ.
4. **Lỗi Quản lý Tài khoản (Account/Pet):**
   - `ERR_ACC_001_EMAIL_EXISTS`: Email đã được đăng ký.
   - `ERR_ACC_002_USER_LOCKED`: Tài khoản đã bị khóa.
   - `ERR_PET_001_NOT_FOUND`: Không tìm thấy thú cưng.

---

## LUẬT THỰC THI (EXECUTION) CHO AI AGENT

Mỗi khi khởi tạo một hàm API (`GET`, `POST`, `PUT`, `DELETE`), AI Agent phải tự nhẩm trong đầu Checklist sau trước khi code:

- [ ] Hàm này trả về `ApiResponse` hay `PageResponse`?
- [ ] DTO request đã có `@Valid` chưa?
- [ ] Các logic `if/else` báo lỗi trong Service đã throw đúng `BusinessException(ErrorCode.XYZ)` chưa?
- [ ] Có bắt phân trang `size <= 100` không?
