---
trigger: manual
---

# [PERFORMANCE-TEST-RULES] MANDATORY PERFORMANCE TESTING STANDARDS FOR PCCMS

> **PURPOSE (MỤC ĐÍCH)**
>
> - Performance Testing nhằm xác minh hệ thống đáp ứng các yêu cầu phi chức năng (Non-Functional Requirements - NFR) về: `Response Time`, `Throughput`, `Concurrency`, `Scalability`, `Resource Utilization`, và `Stability`.
> - Performance Test **KHÔNG** dùng để kiểm tra Business Logic.
> - Performance Test **KHÔNG** thay thế Unit Test hoặc Integration Test.

---

## RULE 1 – NFR FIRST (YÊU CẦU PHI CHỨC NĂNG LÀ TRUNG TÂM)

- KHÔNG ĐƯỢC thực hiện Performance Test nếu chưa có Performance Requirement (NFR).
- Mọi kịch bản Performance Test phải truy vết được về NFR.
- **Ví dụ:**
  - `NFR-001` (Search Medical Records): 95% request < 2 seconds.
  - `NFR-002` (Appointment Booking): 99% request < 3 seconds.
  - `NFR-003` (Concurrent Booking): 500 concurrent users.

---

## RULE 2 – PERFORMANCE TEST DESIGN PROCESS

AI Agent phải thực hiện theo luồng phân tích sau trước khi test:
`NFR Analysis` → `Critical Flow Identification` → `Workload Model Definition` → `Test Scenario Design` → `Test Data Preparation` → `Performance Test Implementation` → `Result Analysis`.

- **CẤM:** Không được viết Performance Test script trực tiếp mà không có thiết kế tải (Workload Model).

---

## RULE 3 – IDENTIFY CRITICAL FLOWS (XÁC ĐỊNH LUỒNG TRỌNG YẾU)

Không cần kiểm thử hiệu năng cho mọi API. Chỉ ưu tiên tập trung vào các luồng có: Tần suất sử dụng cao, Tác động nghiệp vụ lớn, Truy vấn dữ liệu phức tạp.

- Authentication (Login/Token Validation)
- Appointment Booking (Đặt lịch)
- Medical Record Search (Tra cứu bệnh án)
- Invoice Creation (Tạo hóa đơn)
- Payment Processing (Thanh toán)
- Inventory Deduction (Trừ kho thuốc)
- Dashboard Reporting (Báo cáo thống kê)

---

## RULE 4 – PERFORMANCE TEST TYPES (CÁC LOẠI KIỂM THỬ BẮT BUỘC)

### 4.1. Load Test (Kiểm thử chịu tải)

- **Mục tiêu:** Kiểm tra hệ thống hoạt động ổn định ở tải dự kiến.
- **Ví dụ:** 200 concurrent users trong 30 phút.
- **Metrics phải đo:** Response Time, Error Rate, Throughput.

### 4.2. Stress Test (Kiểm thử áp lực)

- **Mục tiêu:** Xác định điểm gãy (Breaking Point) của hệ thống.
- **Ví dụ:** Tăng dần 200 → 500 → 1000 → 2000 users cho đến khi hệ thống sập.
- **Metrics phải đo:** Breaking Point, Failure Mode (Sập do DB hay do hết RAM?).

### 4.3. Spike Test (Kiểm thử đột biến)

- **Mục tiêu:** Đánh giá khả năng chịu tải đột biến.
- **Ví dụ:** 100 users vọt lên 1000 users trong 10 giây (VD: Flash sale dịch vụ).
- **Phải xác minh:** Hệ thống không crash và có khả năng Tự phục hồi (Auto-recovery).

### 4.4. Endurance Test (Kiểm thử độ bền)

- **Mục tiêu:** Kiểm tra rò rỉ tài nguyên (Memory Leak).
- **Ví dụ:** Chạy 200 users liên tục trong 8 giờ hoặc 24 giờ.
- **Phải theo dõi:** Heap Memory, CPU, Database Connections, Thread Count.

### 4.5. Scalability Test (Kiểm thử khả năng mở rộng)

- **Mục tiêu:** Đánh giá độ co giãn.
- **Ví dụ:** Scale CPU (x1, x2, x4) hoặc Scale Instances (1, 2, 4 nodes).
- **Phải đánh giá:** Performance Improvement Ratio (Tỷ lệ cải thiện hiệu năng tương xứng với tài nguyên).

---

## RULE 5 – RESPONSE TIME REQUIREMENTS (KPI VỀ THỜI GIAN PHẢN HỒI)

- **CẤM:** Tuyệt đối không sử dụng Average Response Time (Thời gian phản hồi trung bình) làm KPI chính vì nó che giấu các request bị kẹt (outliers).
- **Bắt buộc sử dụng:** `P50`, `P95`, `P99` (Percentiles).
- **Mặc định PCCMS phải đáp ứng:**
  - `Read APIs`: P95 < 2s, P99 < 3s.
  - `Create/Update APIs`: P95 < 3s, P99 < 5s.
  - `Search APIs`: P95 < 3s, P99 < 5s.
  - `Batch Operations`: Theo đặc tả riêng.

---

## RULE 6 – RESOURCE UTILIZATION (SỬ DỤNG TÀI NGUYÊN)

Performance Test CHỈ ĐẠT nếu Response Time đạt yêu cầu **VÀ** Resource Usage ổn định. Phải theo dõi sát sao:

- CPU Usage & Memory Usage
- JVM Heap Usage & GC Activity (Thời gian dọn rác)
- Database Connections (Connection Pool Leak)
- Virtual Threads & Thread Pools

---

## RULE 7 – DATABASE PERFORMANCE (HIỆU NĂNG CƠ SỞ DỮ LIỆU)

Các truy vấn quan trọng phải được kiểm tra dưới DB:

- `Query Execution Time`
- `Index Usage` (Có đánh Index đúng không?)
- `Lock Wait Time` & `Deadlock Risk`
- **TỐI KỴ & KHÔNG CHẤP NHẬN:** N+1 Query, Full Table Scan ngoài chủ đích, Excessive Join, Cross-Module Join.

---

## RULE 8 – CONCURRENCY TESTING (DƯỚI TẢI THỰC TẾ)

Các chức năng giao dịch tài nguyên BẮT BUỘC phải test đồng thời (Concurrent):

- `Appointment Booking`, `Room Allocation`, `Inventory Deduction`, `Payment Processing`.
- **Phải xác minh:** KHÔNG mất dữ liệu, KHÔNG double booking (đặt trùng), KHÔNG negative inventory (âm kho), KHÔNG race condition.

---

## RULE 9 – TEST ENVIRONMENT (MÔI TRƯỜNG KIỂM THỬ)

- **Cấm:** Không sử dụng môi trường Development đang được chia sẻ (Shared Dev Env) để đo hiệu năng.
- **Cấm:** Không sử dụng H2 Database để đo hiệu năng thay cho PostgreSQL.
- **Bắt buộc:** Môi trường kiểm thử phải có cấu hình gần Production nhất có thể (Cùng loại Database, cùng JVM version, cùng Spring Profile).

---

## RULE 10 – TEST DATA (DỮ LIỆU KIỂM THỬ)

Test Data phải phản ánh khối lượng dữ liệu thực tế (Production-like Data).

- **Ví dụ:** Nếu trên Production dự kiến có 10 triệu Medical Records, thì Database lúc test không được phép chỉ có 100 records.
- Dữ liệu kiểm thử phải đủ lớn để làm bộc lộ các vấn đề về Missing Index và Full Table Scan.

---

## RULE 11 – TOOLING & REPORTING

- **Công cụ ưu tiên:** `JMeter`, `Gatling`, `k6`.
- **Lưu trữ:** Kết quả phải được xuất và lưu trữ tại thư mục `performance-results/`.
- **Bao gồm:** Report tổng hợp, Metrics log, Charts trực quan, và Configuration file của tool test.

---

## RULE 12 – PERFORMANCE REGRESSION (CHỐNG SUY THOÁI HIỆU NĂNG)

- Mỗi Release phải được so sánh với Baseline (Mức cơ sở của bản release trước).
- **Không được chấp nhận (nếu không có phê duyệt từ Architect):**
  - Response Time tăng > 20%.
  - Throughput giảm > 20%.
  - Error Rate tăng.

---

## RULE 13 – SUCCESS CRITERIA (TIÊU CHÍ NGHIỆM THU HIỆU NĂNG)

Một chức năng được xem là đạt yêu cầu hiệu năng khi thỏa mãn:

- [ ] NFR tồn tại và rõ ràng.
- [ ] Workload Model được xác định.
- [ ] Load Test hoàn thành.
- [ ] Stress Test hoàn thành.
- [ ] Spike Test / Endurance Test hoàn thành (nếu NFR yêu cầu).
- [ ] P95 đạt mục tiêu.
- [ ] P99 đạt mục tiêu.
- [ ] Error Rate nằm trong ngưỡng cho phép (< 1% hoặc tùy NFR).
- [ ] Resource Utilization (CPU, RAM, DB Pool) ổn định.
- [ ] KHÔNG phát hiện bottleneck nghiêm trọng.
- [ ] KHÔNG phát hiện race condition dưới áp lực tải.
- [ ] KHÔNG phát hiện memory leak sau thời gian dài.
- [ ] KHÔNG phát hiện database bottleneck (như lock chờ quá lâu).
