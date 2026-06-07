---
trigger: always_on
---

# [TEST-IMPLEMENTATION-RULES] CODING STANDARDS FOR TESTING

> **LƯU Ý CHO AGENT:** Đây là các tiêu chuẩn bắt buộc khi gõ code Java/Spring Boot cho việc kiểm thử. Mọi sai lệch sẽ bị reject.

## RULE 5 & 6 – CSV DRIVEN TEST ARCHITECTURE (Single Source Of Truth)

- **Luật:** Mọi Parameterized Test BẮT BUỘC phải sử dụng dữ liệu bên ngoài. **TUYỆT ĐỐI KHÔNG HARDCODE** dữ liệu test trong source code Java.
- **Vị trí file:** `src/test/resources/testcases/*.csv` (Ví dụ: `pet-age-validation.csv`).
- **Implement Test Logic 1 Lần:** Code test chỉ được viết 1 hàm duy nhất dùng `@ParameterizedTest` và `@CsvFileSource`. Khi có case mới, CẤM sửa code Java, CHỈ ĐƯỢC thêm dòng vào file CSV.
- **Cấu trúc CSV chuẩn:** Bắt buộc có cột `rule_id` để mapping với RULE 7.
  ```csv
  rule_id,case_id,input_age,input_weight,expected_result
  BR-01,TC001,1,5,VALID
  BR-01,TC002,0,5,INVALID
  BR-02,TC003,101,5,INVALID
  ```

## RULE 8 – TEST NAMING CONVENTION

CẤM sử dụng các tên chung chung như `test()`, `test1()`, `createPetTest()`.
BẮT BUỘC sử dụng format: `should_[Expected_Result]_when_[Condition_Or_Action]()`

- _Đúng:_ `should_create_pet_when_all_inputs_are_valid()`
- _Đúng:_ `should_throw_business_exception_when_stock_is_insufficient()`

## RULE 9 – UNIT TEST ISOLATION

- Unit Test CHỈ kiểm thử một Unit (Class).
- Mọi dependency (Repository, External Service) BẮT BUỘC phải được Mock hoặc Stub (dùng `Mockito`).
- CẤM truy cập Database, Network, API ngoài, File System trong quá trình chạy Unit Test.

## RULE 10 – INTEGRATION TEST SEPARATION

- Integration Test (Kiểm thử tích hợp) PHẢI nằm riêng biệt với Unit Test.
- **BẮT BUỘC** sử dụng `Testcontainers` (PostgreSQL Container) kết hợp `@SpringBootTest` hoặc `@DataJpaTest`.
- Mục tiêu: Kiểm tra Repository, Transaction, DB Mapping, Database Locking (`@Lock`).
- CẤM sử dụng Mock thay cho Database thực (Cấm dùng H2 Database in-memory).

## RULE 11 – CONTROLLER TESTING (API Layer)

- Controller BẮT BUỘC phải được kiểm thử bằng `@WebMvcTest` và `MockMvc`.
- **Ngoại lệ:** Nếu môi trường/hệ thống thiếu dependency cho `@WebMvcTest` (VD: `spring-boot-test-autoconfigure`), cho phép sử dụng `MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(...)` để thay thế, nhưng BẮT BUỘC phải gắn kèm GlobalExceptionHandler để test chính xác cấu trúc Response lỗi.
- Mục tiêu: Chỉ kiểm tra HTTP Status (`200`, `400`, `403`), Request Validation (`@Valid`), Cấu trúc Response JSON, và Security Constraints (`@PreAuthorize`).
- CẤM kiểm thử Business Logic (Nghiệp vụ) tại Controller. Logic nghiệp vụ phải được Mock.
