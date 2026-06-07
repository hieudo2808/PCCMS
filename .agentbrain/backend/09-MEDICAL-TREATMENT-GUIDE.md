# [MEDICAL-TREATMENT-GUIDE] ĐẶC TẢ NGHIỆP VỤ KHÁM CHỮA BỆNH & ĐIỀU TRỊ

> **MỤC ĐÍCH TỐI THƯỢNG:** Tài liệu này thiết lập các Business Rules nghiêm ngặt nhất cho phân hệ Y tế (Medical Care Subsystem) của dự án PCCMS. AI Agent **BẮT BUỘC** phải tuân thủ các quy tắc về Khóa bệnh án (Immutability), Trừ tồn kho thuốc an toàn (Concurrency Locking), và Cảnh báo y tế.

---

## RULE 1 – VÒNG ĐỜI VÀ TÍNH BẤT BIẾN CỦA BỆNH ÁN (MEDICAL RECORD IMMUTABILITY)

Bệnh án (`medical_records`) là tài liệu pháp lý y khoa. Hệ thống KHÔNG cho phép sửa đổi tùy tiện.

### 1.1. Các trạng thái của Bệnh án (Status Lifecycle)

- **`DRAFT` (Bản nháp):** Bác sĩ đang trong quá trình khám, nhập sinh hiệu, chờ kết quả xét nghiệm. Ở trạng thái này, Bác sĩ được phép gọi API cập nhật (`PUT / PATCH`) nhiều lần.
- **`FINALIZED` (Đã chốt):** Khám xong, đã kê đơn và đưa ra chẩn đoán cuối cùng.
- **`CANCELLED` (Đã hủy):** Lịch khám bị hủy bỏ.

### 1.2. Ràng buộc Khóa Bệnh Án (Immutability Guard Clause)

Khi Bác sĩ gọi API Update Bệnh án, hệ thống **BẮT BUỘC** phải kiểm tra trạng thái hiện tại dưới Database trước khi thực hiện bất kỳ thay đổi nào:

```java
// Mẫu Guard Clause bắt buộc tại tầng Service
MedicalRecord record = repository.findById(recordId)
    .orElseThrow(() -> new ResourceNotFoundException("Medical record not found"));

if (record.getStatusCode() == RecordStatus.FINALIZED) {
    throw new BusinessException(ErrorCode.ERR_MED_001_RECORD_LOCKED);
}
```

### 1.3. Điều kiện để chốt Bệnh án (Finalization Rules)

API chuyển trạng thái sang `FINALIZED` chỉ thành công khi:

- Trường `final_diagnosis` (Chẩn đoán cuối cùng) KHÔNG được để trống.
- Có ít nhất một chỉ số sinh hiệu (Vitals) được ghi nhận.

---

## RULE 2 – RÀNG BUỘC CHỈ SỐ SINH HIỆU LÂM SÀNG (VITALS VALIDATION)

Hệ thống không được lưu các chỉ số y khoa phi lý. AI Agent BẮT BUỘC phải validate các thông số sau ở cả DTO (`@Min`, `@Max`) và Service:

- **`temperature_c` (Nhiệt độ):** Phải nằm trong giới hạn sống của chó mèo (Ví dụ: 30.0 đến 45.0 độ C).
- **`spo2_percent` (Nồng độ Oxy):** Bắt buộc nằm trong khoảng `0` đến `100`.
- **`heart_rate_bpm` (Nhịp tim) & `respiratory_rate_bpm` (Nhịp thở):** Phải là số dương (> 0).

---

## RULE 3 – KÊ ĐƠN THUỐC & QUẢN LÝ TỒN KHO (PRESCRIPTION & INVENTORY CONCURRENCY)

Đây là nghiệp vụ quan trọng và dễ xảy ra lỗi Race Condition nhất (2 bác sĩ cùng kê 1 loại thuốc đang sắp hết).

### 3.1. Ràng buộc Kê Đơn

- Đơn thuốc (`prescriptions`) BẮT BUỘC phải gắn với một `medical_record_id` có thực và đang ở trạng thái `DRAFT`.
- `quantity` (Số lượng) trong bảng `prescription_items` BẮT BUỘC phải > 0.

### 3.2. Ràng buộc Trừ Kho (Stock Deduction) và Pessimistic Locking

Khi thực hiện lưu `prescription_items`, Agent **BẮT BUỘC** áp dụng luồng sau trong một `@Transactional`:

1. **Lock Database Row:** Query lấy thông tin thuốc (`medicines`) bằng Repository method có gắn `@Lock(LockModeType.PESSIMISTIC_WRITE)`.
   - _Ví dụ:_ `Optional<Medicine> findByIdWithLock(UUID id);`
2. **Kiểm tra tồn kho:**
   ```java
   if (medicine.getCurrentStock() < requestedQuantity) {
       throw new BusinessException(ErrorCode.ERR_MED_002_INSUFFICIENT_STOCK);
   }
   ```
3. **Cập nhật:** Cập nhật `current_stock = current_stock - requestedQuantity` và lưu xuống DB.

---

## RULE 4 – CẢNH BÁO Y TẾ VÀ TIÊM CHỦNG (HEALTH ALERTS & VACCINATIONS)

### 4.1. Tiêm chủng (`vaccination_records`)

- Bắt buộc phải nhập `vaccine_name`.
- `next_due_date` (Ngày hẹn tiêm mũi tiếp theo) BẮT BUỘC phải lớn hơn `vaccination_date`. Ném lỗi nếu nhập sai thời gian.

### 4.2. Cảnh báo Y tế (`health_alerts`)

- Dùng để note lại các tình trạng đặc biệt (VD: Dị ứng kháng sinh, Mắc bệnh Parvovirus lây nhiễm).
- Mọi API lấy chi tiết Thú cưng (`GET /pets/{id}`) BẮT BUỘC phải đính kèm danh sách các `health_alerts` có `resolved_at IS NULL` (cảnh báo đang có hiệu lực) để Bác sĩ nắm được tình trạng khẩn cấp.

---

## RULE 5 – GIAO TIẾP VỚI MODULE KHÁC (CROSS-MODULE EVENT PUBLISHING)

Phân hệ Y tế không được phép gọi trực tiếp Repository của Lễ tân hay Thu ngân. Bắt buộc dùng Spring Application Events.

### 5.1. Đồng bộ với Lịch hẹn (Appointments)

Khi hàm `finalizeMedicalRecord()` chạy thành công, BẮT BUỘC phải gọi:
`applicationEventPublisher.publishEvent(new MedicalRecordCompletedEvent(record.getAppointmentId()));`
_(Để module Lễ tân tự động đổi trạng thái lịch hẹn sang `COMPLETED`)_.

### 5.2. Chuyển thông tin cho Kế toán (Billing)

Khi bác sĩ kê đơn thuốc, hệ thống BẮT BUỘC phải ghi nhận giá bán (`unit_price_vnd`) tại thời điểm hiện tại của thuốc đó, để Module Billing lấy đúng giá đó sinh ra `invoice_lines`. Không được lưu tham chiếu đến giá catalog vì giá thuốc có thể thay đổi trong tương lai.

---

## LUẬT THỰC THI (EXECUTION) DÀNH CHO AI AGENT

Mỗi khi khởi tạo một hàm API trong phân hệ Y tế, AI Agent phải tự nhẩm trong đầu Checklist sau:

- [ ] Hàm Update này đã check `FINALIZED` để chặn sửa đổi chưa?
- [ ] API lấy thuốc từ kho đã dùng `@Lock(LockModeType.PESSIMISTIC_WRITE)` để chống Race Condition chưa?
- [ ] Khách yêu cầu kê đơn số lượng âm hoặc SpO2 = 150 thì đã ném `BusinessException` chưa?
- [ ] Hàm thực thi đã có `@Transactional` bao bọc chưa?
