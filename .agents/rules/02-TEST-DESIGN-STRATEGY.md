---
trigger: always_on
---

# [TEST-DESIGN-STRATEGY] ISTQB TEST DESIGN TECHNIQUES

> **LƯU Ý CHO AGENT:** Hãy áp dụng các kỹ thuật dưới đây để tạo dữ liệu kiểm thử (Test Data) BẤT CỨ KHI NÀO bạn được yêu cầu viết Unit Test cho tầng Service.

## RULE 2 – BLACK BOX TESTING IS MANDATORY

Mọi Service chứa Business Logic bắt buộc phải có kiểm thử hộp đen. Áp dụng nghiêm ngặt các kỹ thuật sau:

### 2.1 Equivalence Partitioning (EP - Phân lớp tương đương)

Phân chia dữ liệu đầu vào thành các lớp. CHỈ CHỌN đại diện tối thiểu cho mỗi lớp. KHÔNG kiểm thử mọi giá trị.

- _Ví dụ (Age):_ Invalid Low (<0) | Valid Range (0-20) | Invalid High (>20).

### 2.2 Boundary Value Analysis (BVA - Phân tích giá trị biên)

Mọi trường dữ liệu có giới hạn BẮT BUỘC phải kiểm tra giá trị biên.

- Tối thiểu phải test: `Min - 1`, `Min`, `Min + 1`, `Max - 1`, `Max`, `Max + 1`.
- _Ví dụ (Quantity 1-100):_ Bắt buộc test các giá trị: `0, 1, 2, 99, 100, 101`.

### 2.3 Decision Table Testing (Bảng quyết định)

Áp dụng khi nghiệp vụ có nhiều điều kiện kết hợp (VD: Role + Status + Payment State).

- Mọi rule nghiệp vụ phải xuất hiện ít nhất một lần trong Decision Table.
- Số lượng testcase phải tối thiểu nhưng bao phủ toàn bộ luật nghiệp vụ.

### 2.4 State Transition Testing (Kiểm thử chuyển trạng thái)

Áp dụng cho mọi Entity có Workflow/Status (VD: DRAFT -> CONFIRMED -> COMPLETED).

- Phải kiểm thử cả **Valid Transitions** (chuyển đúng) và **Invalid Transitions** (chuyển sai, nhảy cóc bước).

### 2.5 Pairwise Testing

Khi có quá nhiều tham số kết hợp (VD: Role x Pet Type x Payment Method), KHÔNG tạo toàn bộ tổ hợp. Dùng Pairwise để giảm số lượng testcase nhưng vẫn đảm bảo phát hiện lỗi tương tác.

---

## RULE 3 – WHITE BOX TESTING IS MANDATORY

Sau khi hoàn thành Black Box, Agent phải đối chiếu lại với code để đảm bảo:

- **Service Layer, Utility, Helper:** BẮT BUỘC đạt **100% Branch Coverage (C1)**. Mọi nhánh `if`, `else`, `switch`, `case`, `ternary operator` phải được thực thi ít nhất 1 lần.
- **DTO, Entity:** KHÔNG yêu cầu kiểm thử.

---

## RULE 4 – MUTATION TESTING AWARENESS (Anti-Fragile Tests)

AI Agent phải thiết kế testcase đủ "nhạy" để phát hiện các lỗi Mutation (lỗi code bị thay đổi ngớ ngẩn).

- Test phải thất bại ngay lập tức nếu Dev sửa code từ `>` thành `>=`, `<` thành `<=`, `==` thành `!=`, `true` thành `false`.
- Viết testcase chỉ để pass lấy coverage nhưng không phát hiện được mutation là KHÔNG ĐƯỢC CHẤP NHẬN.

---

## RULE 7 – REQUIREMENT TRACEABILITY

Mỗi testcase phải truy vết được về Business Rule gốc. Phải có ID của Rule đi kèm (Ví dụ: `BR-01`, `BR-02`). Tồn tại testcase không rõ Business Rule là vi phạm nghiêm trọng.
