---
trigger: always_on
---

# [INTEGRATION-TEST-RULES] MANDATORY INTEGRATION TESTING STANDARDS FOR PCCMS

> **PURPOSE (MỤC ĐÍCH)**
>
> - Integration Test nhằm xác minh các thành phần của hệ thống hoạt động chính xác khi tích hợp với nhau trong môi trường thực tế.
> - Integration Test **không kiểm tra Business Logic chi tiết** như Unit Test.
> - Tích hợp tập trung kiểm tra: `Service ↔ Repository`, `Repository ↔ Database`, `Transaction ↔ Persistence`, `Event ↔ Listener`, `Entity ↔ Database Mapping`, `Security ↔ Request Processing`, `Module ↔ Module Communication`.
> - Mục tiêu là phát hiện lỗi tích hợp mà Unit Test không thể phát hiện.

---

## RULE 1 – TEST THE INTEGRATION POINT, NOT THE BUSINESS RULE

- **Unit Test** kiểm tra: Business Rule, Validation Logic, Calculation Logic.
- **Integration Test** kiểm tra: Integration Point.
  - _Ví dụ Unit Test:_ Kiểm tra logic tính toán số tiền hóa đơn (Invoice amount calculation).
  - _Ví dụ Integration Test:_ Xác minh hóa đơn (Invoice) được lưu chính xác xuống PostgreSQL với đầy đủ các field.
- **CẤM:** Không được lặp lại toàn bộ bộ testcase của Unit Test mang sang Integration Test.

---

## RULE 2 – WHEN INTEGRATION TEST IS MANDATORY

AI Agent **bắt buộc** phải tạo Integration Test khi xuất hiện ít nhất một trong các trường hợp sau:

### 2.1. Custom Repository Query

- **Bao gồm:** `@Query`, JPQL, Native Query, Criteria API, Specification.
- **Phải xác minh:** Query trả dữ liệu đúng, Filter hoạt động đúng, Pagination và Sorting hoạt động đúng.

### 2.2. Transaction Management

- **Bao gồm:** `@Transactional`, Nested Transaction, Rollback Logic.
- **Phải xác minh:** Commit thành công khi hợp lệ, Rollback khi có Exception, KHÔNG tồn tại dữ liệu rác trong DB sau khi Rollback.

### 2.3. Soft Delete

- **Bao gồm:** Các cờ `isActive`, `statusCode`, `deletedFlag`.
- **Phải xác minh:** Dữ liệu không bị xóa vật lý (còn tồn tại dưới DB), Query thông thường chỉ trả về dữ liệu active, dữ liệu soft delete không bị query nhầm.

### 2.4. Optimistic Locking

- **Bao gồm:** `@Version`.
- **Phải xác minh:** Phát hiện Concurrent Update, `OptimisticLockException` được ném ra đúng lúc.

### 2.5. Pessimistic Locking

- **Bao gồm:** `LockModeType.PESSIMISTIC_WRITE`.
- **Phải xác minh:** Record thực sự được khóa trên DB, Concurrent transaction bị chặn (block) hoặc timeout.

### 2.6. Event Publishing

- **Bao gồm:** `ApplicationEventPublisher`, Domain Events, Integration Events.
- **Phải xác minh:** Event được publish, Listener nhận và xử lý đúng, Event **chỉ được publish sau commit** (nếu dùng `@TransactionalEventListener`).
- _Cấm:_ Không được chỉ `verify(mockPublisher)` là xong. Phải kiểm tra hành vi thực tế của hệ thống.

### 2.7. Entity Mapping

- **Bao gồm:** UUID, Enum, LocalDate(Time), BigDecimal, JSON Column, Embedded Object.
- **Phải xác minh:** Persist thành công, Read thành công, giá trị không bị sai lệch (đặc biệt với JSON và Timezone).

### 2.8. Database Constraint

- **Bao gồm:** Unique Constraint, Foreign Key, Check Constraint.
- **Phải xác minh:** Constraint hoạt động chính xác (chặn dữ liệu sai ở tầng DB), ném đúng Exception (như `DataIntegrityViolationException`).

### 2.9. Security Integration

- **Bao gồm:** JWT Authentication, RBAC Authorization, Spring Security Filter.
- **Phải xác minh:** Authorized Request (HTTP 200/201), Unauthorized Request (HTTP 401), Forbidden Request (HTTP 403).

### 2.10. Inter-Module Communication

- **Bao gồm:** Facade Service, Domain Service, Shared Service.
- **Phải xác minh:** Module A gọi được Module B, Contract/Interface không bị vi phạm.
- _Cấm:_ Tuyệt đối KHÔNG ĐƯỢC truy cập trực tiếp Repository của module khác.

---

## RULE 3 – TEST DESIGN SOURCE

- Nguồn sinh Integration Test Case phải từ: **Integration Point Analysis**.
- KHÔNG PHẢI TỪ: Source Code Coverage, Branch Coverage, Random Scenario.
- **Quy trình:** `Identify Integration Point` → `Identify Failure Point` → `Design Test Case` → `Create Test Data` → `Implement Test`.

---

## RULE 4 – TEST DATA MANAGEMENT

- **Cấm:** Không hardcode dữ liệu trong test source code.
- **Bắt buộc sử dụng:** CSV, SQL Seed, Builder Pattern, Factory.
- **Ưu tiên đặt tại:** `src/test/resources/integration-test-data/` (Ví dụ: `invoice-data.csv`, `appointment-data.sql`).

---

## RULE 5 – DATABASE ENVIRONMENT

- **Cấm:** Không sử dụng Production Database, Shared Development Database.
- **Bắt buộc sử dụng:** `Testcontainers` (Ví dụ: `PostgreSQLContainer`).
- Mỗi bài test phải chạy độc lập, không phụ thuộc vào dữ liệu tồn tại từ trước trong hệ thống.

---

## RULE 6 – TEST ISOLATION (TÍNH CÔ LẬP)

- Mỗi Integration Test phải: Độc lập (Independent), Lặp lại được (Repeatable), Kết quả tất định (Deterministic).
- **Không phụ thuộc vào:** Thời gian thực (Current Time), Mạng bên ngoài (External Network), Bản ghi DB cũ (Existing Database Records), Kết quả của bài test trước đó (Previous Test Execution).

---

## RULE 7 – TRANSACTION TESTING

Bắt buộc kiểm tra cả hai luồng:

1. **Success Path (Transaction Commit):** `Create Invoice` → `Save Invoice` → `Save Payment` → `Commit` → `Xác minh Data Exists dưới DB`.
2. **Failure Path (Transaction Rollback):** `Create Invoice` → `Save Invoice` → `Save Payment` → `Throw Exception` → `Rollback` → `Xác minh No Data Exists dưới DB`.

---

## RULE 8 – EVENT TESTING

Nếu Service publish Event, phải xác minh:

- Event được publish.
- Listener được gọi.
- Side Effect (Tác động phụ) xảy ra.
- Event chỉ xuất hiện sau khi transaction đã commit.
- **Cấm:** Không sử dụng `verify(eventPublisher)` để qua mặt. Phải xác minh kết quả thực tế của Listener.

---

## RULE 9 – CONCURRENCY TESTING (KIỂM THỬ ĐỒNG THỜI)

Nếu sử dụng: `@Version`, DB Lock, Inventory Allocation (Cấp phát kho), Room Allocation (Cấp phát phòng).

- **Phải có Concurrent Integration Test.**
- **Xác minh:** Chống Race Condition thành công, Lock Behaviour hoạt động đúng, Tính nhất quán của dữ liệu (Data Consistency) được bảo toàn.

---

## RULE 10 – TEST NAMING CONVENTION

- **Không sử dụng:** `test1()`, `testRepository()`, `integrationTest()`.
- **Bắt buộc dùng mẫu:**
  - `should_persist_invoice_when_transaction_commits()`
  - `should_rollback_transaction_when_payment_creation_fails()`
  - `should_throw_optimistic_lock_exception_when_record_is_updated_concurrently()`

---

## RULE 11 – FORBIDDEN PRACTICES (CÁC HÀNH VI BỊ NGHIÊM CẤM)

AI Agent tuyệt đối **CẤM** thực hiện:

- [x] Mock Repository trong Integration Test.
- [x] Mock Database.
- [x] Mock Transaction Manager.
- [x] Mock EntityManager.
- [x] Mock Event Listener cần kiểm tra thực tế.
- [x] Sử dụng H2 in-memory DB để thay thế PostgreSQL nếu hệ thống dùng PostgreSQL.
- [x] Phụ thuộc dữ liệu tồn tại sẵn.

---

## RULE 12 – SUCCESS CRITERIA

Một Integration Test được xem là đạt khi thỏa mãn toàn bộ checklist sau:

- [ ] Integration Point được xác định rõ.
- [ ] Failure Point được xác định rõ.
- [ ] Testcontainers được sử dụng.
- [ ] Database thực được sử dụng.
- [ ] Transaction (Commit) được kiểm tra.
- [ ] Rollback được kiểm tra.
- [ ] Event được kiểm tra.
- [ ] Locking được kiểm tra khi có sử dụng.
- [ ] Security được kiểm tra khi có sử dụng.
- [ ] Test độc lập hoàn toàn.
- [ ] Không sử dụng Mock cho thành phần cần tích hợp thực.
- [ ] Có khả năng chạy lặp lại nhiều lần với cùng kết quả.

> **LƯU Ý CUỐI:** Một chức năng CHỈ cần Integration Test nếu tồn tại Integration Point đáng kiểm tra. KHÔNG BẮT BUỘC viết Integration Test cho mọi Service hoặc mọi Class.
