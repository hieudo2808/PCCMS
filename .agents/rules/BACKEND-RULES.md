---
trigger: always_on
---

# [BACKEND-RULES] QUY CHUẨN LẬP TRÌNH BACKEND (JAVA 25 & SPRING BOOT 4.0.5)

> **MỤC ĐÍCH:** Tài liệu này thiết lập các tiêu chuẩn kỹ thuật bắt buộc cho tầng Backend của dự án PCCMS. AI Agent **PHẢI** tuân thủ mọi quy tắc ở đây khi sinh code Java.

---

## 1. CẤU TRÚC GÓI (PACKAGE STRUCTURE)

Hệ thống sử dụng kiến trúc phân lớp (Layered Architecture) bên trong từng Module.
Cấu trúc chuẩn của một Module phải bao gồm:

- `controller/`: Chứa các REST Controller.
- `service/`: Chứa các Service Interface.
- `service/impl/`: Chứa các class implement Service.
- `repository/`: Chứa các interface kế thừa `JpaRepository`.
- `entity/`: Chứa các Entity (Ánh xạ Database).
- `dto/request/`: Chứa các DTO nhận dữ liệu đầu vào.
- `dto/response/`: Chứa các DTO trả dữ liệu đầu ra.
- `mapper/`: Chứa các Interface của MapStruct để map giữa Entity và DTO.

**TUYỆT ĐỐI KHÔNG** tạo các package nằm ngoài luồng này trừ khi được yêu cầu rõ ràng.

---

## 2. QUY CHUẨN TẦNG DOMAIN & ENTITY (DATABASE MAPPING)

### 2.1. Cấu trúc Entity

- Bắt buộc dùng `java.util.UUID` làm kiểu dữ liệu cho Khóa chính (Primary Key).
  - Khai báo: `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
- Mọi Entity bảng chính (Master Data) **BẮT BUỘC** kế thừa `com.pccms.common.domain.AuditableEntity` (để tự động có các trường `createdAt`, `updatedAt`, `createdBy`, `updatedBy`).
- **Đặt tên bảng & cột:** Dùng `@Table(name = "tên_bảng_dạng_snake_case_số_nhiều")` và `@Column(name = "tên_cột_snake_case")`.

### 2.2. Xóa Mềm (Soft Delete)

- **CẤM** sử dụng các câu lệnh `DELETE` cứng trên Database.
- Mọi Entity đều phải có cờ trạng thái, ví dụ: `@Column(name = "is_active") private Boolean isActive = true;` hoặc `@Column(name = "status_code") @Enumerated(EnumType.STRING) private Status status;`
- Các thao tác "xóa" trong Service phải là update cờ này thành `false` hoặc `INACTIVE`.
- Mọi câu lệnh Query trong Repository (như `findAll`) phải tự động gắn điều kiện `findByIsActiveTrue()`.

---

## 3. QUY CHUẨN TẦNG DTO & MAPPER

### 3.1. Data Transfer Objects (DTO)

- **CẤM TRẢ ENTITY RA NGOÀI CONTROLLER.** Mọi dữ liệu giao tiếp với Frontend bắt buộc phải thông qua DTO.
- **Dùng Java Record:** Từ Java 14 trở lên (dự án dùng Java 25), BẮT BUỘC sử dụng `record` thay cho `class` đối với mọi DTO để đảm bảo tính bất biến (Immutability).
  - _Ví dụ:_ `public record PetProfileResponse(UUID id, String name, String breed) {}`
- **Validation:** Các `Request DTO` bắt buộc phải gắn annotation của `jakarta.validation.constraints` (`@NotBlank`, `@NotNull`, `@Min`, `@Size`, `@Email`).

### 3.2. Mapper (MapStruct)

- Dùng thư viện `MapStruct` để tự động map dữ liệu giữa DTO và Entity. Không viết code map tay (ví dụ `dto.setName(entity.getName())`) dài dòng trừ những trường hợp logic mapping quá phức tạp.

### 3.3. Quy tắc Trình bày mã (Formatting) & Imports

- **STRICT IMPORT RULE (CẤM DÙNG FQCN INLINE):** Tuyệt đối không được viết đường dẫn class đầy đủ (Fully Qualified Class Name) bên trong thân code.
  - _Sai:_ `org.springframework.data.domain.Pageable pageable = ...`
  - _Đúng:_ `Pageable pageable = ...` (Và phải khai báo `import org.springframework.data.domain.Pageable;` ở đầu file).
- **Full File Output:** Khi tạo class mới hoặc cập nhật một class có nhiều dependency mới, Agent BẮT BUỘC phải output toàn bộ nội dung file, bao gồm cả package declaration và khối `import` ở trên cùng. Không được sinh code chắp vá.

---

## 4. QUY CHUẨN TẦNG REPOSITORY (DATA ACCESS)

- Bắt buộc kế thừa `org.springframework.data.jpa.repository.JpaRepository`.
- **Hiệu năng:** Khi truy vấn các danh sách có quan hệ (OneToMany, ManyToOne) cần trả về Frontend, BẮT BUỘC dùng `@Query` kết hợp `JOIN FETCH` hoặc dùng `@EntityGraph` để tránh lỗi **N+1 Query**.
- **Tính đồng thời (Concurrency):** Khi thao tác cập nhật dữ liệu nhạy cảm (như trừ kho thuốc `Medicine`, cập nhật phòng trống `RoomAllocation`), phải sử dụng Pessimistic Locking: `@Lock(LockModeType.PESSIMISTIC_WRITE)`.

---

## 5. QUY CHUẨN TẦNG SERVICE (BUSINESS LOGIC)

Đây là nơi chứa toàn bộ "não bộ" của dự án. Agent phải cực kỳ khắt khe ở tầng này.

### 5.1. Transactional

- Mọi class `ServiceImpl` phải được gắn annotation `@Transactional(readOnly = true)` ở mức Class.
- Mọi hàm làm thay đổi dữ liệu (Create, Update, Delete) **BẮT BUỘC** gắn `@Transactional` (sẽ ghi đè readOnly ở mức class).

### 5.2. Guard Clauses & Ném Ngoại Lệ

- Không dùng khối `if-else` lồng nhau quá sâu (Arrow Anti-Pattern). Hãy dùng Guard Clauses: kiểm tra lỗi và ném Exception ngay ở đầu hàm.
- Mọi vi phạm nghiệp vụ (Business Rule) phải ném ra `com.pccms.common.exception.BusinessException` kèm một `ErrorCode` (Enum).
- _Ví dụ code chuẩn:_

  ```java
  public MedicalRecordResponse finalizeRecord(UUID recordId) {
      MedicalRecord record = repository.findById(recordId)
          .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord not found"));

      if (record.getStatus() == RecordStatus.FINALIZED) {
          throw new BusinessException(ErrorCode.RECORD_ALREADY_FINALIZED);
      }

      record.setStatus(RecordStatus.FINALIZED);
      return mapper.toResponse(repository.save(record));
  }
  ```

### 5.3. Hạn chế Logic trong Query

- **CẤM** sử dụng các hàm String (như `LIKE`, `LOWER`, `UPPER`) ngay trong `@Query` JPA nếu không dùng `nativeQuery=true`.
- Nếu cần tìm kiếm linh động, hãy để logic tìm kiếm (String manipulation) trong Service hoặc DTO, sau đó build Predicate bằng `Specification`.
  - _Lý do:_ Để tách biệt logic xử lý dữ liệu (Java) và logic truy vấn database (SQL).

---

## 6. QUY CHUẨN TẦNG CONTROLLER (REST API)

- Lớp Controller chỉ làm 3 việc: Nhận Request, Gọi Service, Trả về Response. **TUYỆT ĐỐI KHÔNG** viết logic nghiệp vụ (if/else check dữ liệu DB, gán giá trị Entity) ở Controller.
- Sử dụng Constructor Injection bằng từ khóa `final` hoặc `@RequiredArgsConstructor` (của Lombok nếu có cấu hình). Không dùng `@Autowired` lên các field.
- **Chuẩn hóa Đường dẫn API (Naming RESTful URI):**
- Luôn viết thường (lowercase), dùng dấu gạch ngang (kebab-case). Danh từ ở dạng số nhiều.
  _Đúng:_ `/api/v1/medical-records`, `/api/v1/pets/{petId}/prescriptions`
  _Sai:_ `/api/v1/MedicalRecord`, `/api/v1/getPet`

- **Chuẩn hóa Dữ liệu Trả về (Response Wrapper):**
  Mọi API trả về **BẮT BUỘC** phải được bọc trong class `com.pccms.common.response.ApiResponse<T>`.
  Mọi API trả về danh sách phân trang **BẮT BUỘC** dùng `com.pccms.common.response.PageResponse<T>`.

---

## 7. QUY CHUẨN BẢO MẬT BÊN TRONG CODE (SECURITY INJECTION)

- Khi thực thi các logic liên quan đến dữ liệu cá nhân (ví dụ: Chủ nuôi A lấy danh sách thú cưng, xem bệnh án), Agent **BẮT BUỘC** phải chèn logic kiểm tra quyền sở hữu (Ownership validation).
- Dữ liệu `accountId` đang request phải khớp với `ownerId` của Entity lấy dưới DB lên. Nếu không khớp, ném `new BusinessException(ErrorCode.FORBIDDEN_ACCESS)`.

> **XÁC NHẬN CỦA AGENT:** Mọi mã Java Spring Boot được sinh ra sau khi nạp tệp này sẽ tự động áp dụng `Record` cho DTO, bọc `ApiResponse`, bắt lỗi bằng `BusinessException` và chống N+1 Query.
