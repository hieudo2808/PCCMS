---
trigger: always_on
---

# [API-STANDARDS] QUY CHUẨN THIẾT KẾ & GIAO TIẾP RESTful API (PCCMS)

> **MỤC ĐÍCH:** Tài liệu này là "Hợp đồng giao tiếp" (API Contract) giữa Frontend và Backend. Bất kỳ API nào do AI Agent sinh ra **BẮT BUỘC** phải tuân thủ nghiêm ngặt các quy tắc định tuyến, phương thức HTTP, và cấu trúc JSON phản hồi dưới đây.

---

## 1. QUY CHUẨN ĐẶT TÊN ENDPOINT (URI NAMING CONVENTIONS)

- **Danh từ số nhiều (Plural Nouns):** URI BẮT BUỘC phải dùng danh từ số nhiều để chỉ định tài nguyên, tuyệt đối không dùng động từ.
- **Kebab-case:** Dùng chữ thường và dấu gạch ngang để ngăn cách các từ.
- **Versioning:** Luôn bắt đầu bằng `/api/v1/`.
- **Phân cấp tài nguyên (Hierarchical):** Thể hiện rõ mối quan hệ cha-con.
  - _Đúng:_ `GET /api/v1/users/{userId}/pets` (Lấy danh sách thú cưng của user)
  - _Sai:_ `GET /api/v1/getPetsByUser?id=...`
  - _Đúng:_ `POST /api/v1/medical-records/{recordId}/prescriptions`
  - _Sai:_ `POST /api/v1/createPrescription`

---

## 2. SỬ DỤNG ĐÚNG PHƯƠNG THỨC HTTP (HTTP METHODS)

Agent BẮT BUỘC phải map đúng hành động nghiệp vụ với phương thức HTTP:

- **`GET`**: Lấy dữ liệu (Không bao giờ làm thay đổi trạng thái hệ thống). Truyền tham số qua `@PathVariable` hoặc `@RequestParam`.
- **`POST`**: Tạo mới tài nguyên. Dữ liệu truyền qua `@RequestBody`.
- **`PUT`**: Cập nhật **toàn bộ** tài nguyên (Ghi đè).
- **`PATCH`**: Cập nhật **một phần** tài nguyên (Ví dụ: Chỉ đổi trạng thái bệnh án từ DRAFT sang FINALIZED, khóa tài khoản).
- **`DELETE`**: Xóa tài nguyên (Hệ thống dùng Soft-delete, nhưng endpoint vẫn dùng DELETE method).

---

## 3. CẤU TRÚC JSON PHẢN HỒI (RESPONSE WRAPPER)

Để Frontend dễ dàng xử lý (parse) dữ liệu, **TUYỆT ĐỐI KHÔNG** trả về dữ liệu thô (raw data) hoặc String. Mọi API trả về status `200 OK` đều phải được bọc trong class `ApiResponse<T>` hoặc `PageResponse<T>`.

### 3.1. Phản hồi thành công thông thường (ApiResponse)

```json
{
  "success": true,
  "code": 200,
  "message": "Thao tác thành công",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Milo"
  }
}
```

### 3.2. Phản hồi danh sách phân trang (PageResponse)

Khi Frontend gọi danh sách (vd: lịch sử khám, danh sách thuốc), bắt buộc dùng phân trang.

```json
{
  "success": true,
  "code": 200,
  "message": "Lấy danh sách thành công",
  "data": {
    "content": [ ... ],
    "pageNumber": 1,
    "pageSize": 20,
    "totalElements": 150,
    "totalPages": 8,
    "isLast": false
  }
}

```

---

## 4. QUẢN LÝ LỖI & MÃ TRẠNG THÁI HTTP (ERROR HANDLING & STATUS CODES)

Agent KHÔNG ĐƯỢC luôn luôn trả về `200 OK` nếu logic có lỗi. Phải dùng đúng mã HTTP.

### Bảng Mã HTTP Bắt Buộc:

- **`200 OK`**: Get, Put, Patch, Delete thành công.
- **`201 Created`**: Post tạo mới thành công.
- **`400 Bad Request`**: Dữ liệu gửi lên sai định dạng, thiếu field (`@Valid` failed), hoặc vi phạm nghiệp vụ (Ví dụ: Số lượng kê đơn vượt quá tồn kho).
- **`401 Unauthorized`**: Token thiếu, sai, hoặc đã hết hạn.
- **`403 Forbidden`**: Có Token hợp lệ nhưng KHÔNG CÓ QUYỀN thao tác (VD: User thường cố sửa bệnh án, hoặc Chủ nuôi A cố xem thú cưng của Chủ nuôi B).
- **`404 Not Found`**: Không tìm thấy resource (Entity không tồn tại, hoặc đã bị soft-delete).
- **`409 Conflict`**: Xung đột dữ liệu (VD: Đăng ký trùng Email/Phone, hoặc Đặt trùng phòng lưu trú trong cùng khung giờ).
- **`500 Internal Server Error`**: Lỗi hệ thống, sập DB (Frontend sẽ hiển thị "Lỗi hệ thống, vui lòng thử lại").

### Cấu trúc JSON khi có lỗi (Xử lý bởi GlobalExceptionHandler):

```json
{
  "success": false,
  "code": 400,
  "message": "Số lượng thuốc trong kho không đủ.",
  "errorCode": "INSUFFICIENT_STOCK",
  "errors": null
}
```

_(Nếu là lỗi Validate Form, trường `errors` sẽ chứa mảng các field bị lỗi để UI bôi đỏ)_.

---

## 5. QUY CHUẨN DTO & BẢO VỆ DỮ LIỆU (DATA CONTRACTS)

- **Tách biệt Request / Response:** Tuyệt đối không dùng chung 1 DTO cho cả chiều gửi lên và trả về.
- Hậu tố bắt buộc: `...Request` (vd: `CreatePetRequest`) và `...Response` (vd: `PetProfileResponse`).

- **Không Trust Client:** Mọi field id nhạy cảm như `accountId` của người đang đăng nhập KHÔNG ĐƯỢC truyền từ Frontend qua Body Request. Backend BẮT BUỘC phải lấy từ `SecurityContextHolder` (JWT Token) để chống hack.
- **Lọc dữ liệu nhạy cảm:** Các `ResponseDTO` tuyệt đối không chứa `passwordHash`, `resetToken`, hoặc các thông tin bảo mật nội bộ khác.

---

## 6. TÀI LIỆU HÓA API (SWAGGER / OPENAPI 3.0)

Agent khi viết Controller bắt buộc phải gắn annotation của thư viện `springdoc-openapi` để tự động sinh tài liệu API cho Frontend Dev đọc.

- Bọc Controller bằng `@Tag(name = "Medical Record Management", description = "...")`.
- Bọc mỗi endpoint bằng `@Operation(summary = "Tạo bệnh án mới", description = "...")`.

> **XÁC NHẬN CỦA AGENT:** Bằng việc nạp file này, Agent cam kết mọi Controller sinh ra đều sử dụng đúng HTTP Method, DTO Request/Response tách biệt, trả về đúng format `ApiResponse`, và phân luồng lỗi HTTP chính xác.
