# BÁO CÁO KIỂM THỬ BACKEND SPRING BOOT

## Thông tin tài liệu

| Thuộc tính | Giá trị |
| --- | --- |
| Hệ thống | PCCMS |
| Phạm vi | Backend Spring Boot và bộ kiểm thử tại `backend/src/test` |
| Nhánh kiểm tra | `staging-v0.1` |
| Commit nền | `5ccb1b5` và các thay đổi chưa commit trong working tree |
| Ngày thực hiện | 11/06/2026, múi giờ UTC+07:00 |
| Người thực hiện | Codex review trên môi trường phát triển |
| Trạng thái đánh giá | **Đạt có điều kiện đối với regression suite; chưa đủ điều kiện ký duyệt QA toàn diện** |

## Tóm tắt điều hành

Bộ kiểm thử backend có độ phủ chức năng rộng, bao gồm 18 module nghiệp vụ, 71 tệp Java trong thư mục test và 35 tệp dữ liệu CSV với 329 dòng testcase. Các test sử dụng JUnit, Mockito, AssertJ, MockMvc, Spring Security Test, Spring Boot Test, Testcontainers PostgreSQL và JaCoCo.

Kết quả chạy lệnh `mvn verify` ngày 11/06/2026:

| Chỉ số | Kết quả |
| --- | ---: |
| Lớp test được Maven thực thi | 68 |
| Testcase được ghi nhận | 429 |
| Pass | 427 |
| Failure | 0 |
| Error | 0 |
| Skipped | 2 |
| Thời gian Maven | 1 phút 17 giây |
| Line coverage | 45,74% |
| Branch coverage | 31,98% |

Tuy nhiên, kết quả xanh trên chưa phản ánh toàn bộ test trong mã nguồn:

- Hai lớp có hậu tố `*IT` không được `mvn verify` thực thi do chưa cấu hình Maven Failsafe hoặc quy tắc include tương ứng.
- Khi chạy riêng hai lớp này, `MedicineRepositoryIT` pass 2 test nhưng `PrescriptionConcurrencyIT` lỗi tại bước tạo dữ liệu role do thiếu trường bắt buộc `name`.
- CI hiện chỉ chạy `clean compile`, không chạy `test` hoặc `verify`.
- Một số `@SpringBootTest` không dùng datasource test độc lập và trong lần review đã kết nối database local.
- Có ít nhất 30 lượt chạy parameterized test được tính là pass nhưng thoát sớm bằng `return`, không thực hiện assertion hoặc hành vi cần kiểm tra.
- Chưa có Performance Test, Postman/Newman collection hoặc OWASP ZAP/DAST trong repository.

Kết luận quản trị: bộ test hiện tại hữu ích cho regression ở tầng service và controller, nhưng chưa thể dùng làm bằng chứng duy nhất để xác nhận chất lượng tích hợp, hiệu năng và an toàn bảo mật trước phát hành.

---

## 1. Giới thiệu

### 1.1. Mục đích kiểm thử

Báo cáo này đánh giá:

- Mức độ hiện thực hóa các testcase backend.
- Kết quả chạy tự động thực tế.
- Mức bao phủ mã nguồn.
- Khả năng lặp lại và mức cô lập của môi trường test.
- Khoảng trống giữa tài liệu testcase, mã test và pipeline CI.
- Rủi ro cần xử lý trước khi QA ký duyệt.

### 1.2. Phạm vi kiểm thử

Phạm vi bao gồm:

- Logic nghiệp vụ trong service.
- Controller và hợp đồng HTTP.
- Repository và database PostgreSQL.
- Xác thực, phân quyền và filter bảo mật.
- Validation, exception và error code.
- Transaction và một số tình huống đồng thời.
- Báo cáo coverage bằng JaCoCo.

Ngoài phạm vi:

- Frontend/UI/UX.
- Kiểm thử trên môi trường staging triển khai thực tế.
- Load, stress, soak và capacity test.
- DAST bằng OWASP ZAP.
- Kiểm thử API ngoài tiến trình bằng Postman/Newman.

### 1.3. Loại kiểm thử được ghi nhận

| Loại kiểm thử | Hiện trạng |
| --- | --- |
| Unit Test | Có, chủ yếu service/entity/mapper/helper với Mockito |
| Integration Test | Có một phần; pipeline bỏ sót hai lớp `*IT` |
| API/Controller Test | Có, chủ yếu MockMvc `standaloneSetup` |
| Security Test | Có kiểm tra authentication/authorization ở một số module |
| Performance Test | Chưa có |
| DAST/Security scanning | Chưa có |

---

## 2. Môi trường kiểm thử

### 2.1. Công nghệ và công cụ

| Thành phần | Phiên bản/cấu hình quan sát được |
| --- | --- |
| Java | Amazon Corretto 25.0.2 |
| Maven | 3.9.14 |
| Spring Boot | 4.0.5 |
| JUnit Jupiter | Được cung cấp qua Spring Boot Test |
| Mockito | Được cung cấp qua Spring Boot Test |
| AssertJ | Được cung cấp qua Spring Boot Test |
| Testcontainers | PostgreSQL 15/16 Alpine |
| JaCoCo | 0.8.13 |
| Hệ điều hành | Windows 11 x64 |
| Docker khi chạy IT thủ công | Docker Desktop, server 29.5.2 |

### 2.2. Lệnh xác minh

Lệnh regression chính:

```powershell
mvn verify
```

Lệnh chạy riêng các integration test bị bỏ sót:

```powershell
mvn "-Dtest=MedicineRepositoryIT,PrescriptionConcurrencyIT" test
```

Maven Wrapper `mvnw.cmd` không khởi động được trong sandbox review, do đó báo cáo sử dụng Maven 3.9.14 đã được wrapper tải sẵn. Đây được ghi nhận là giới hạn môi trường review, chưa kết luận là lỗi trên mọi máy phát triển.

### 2.3. Cấu hình môi trường test

Tệp [`backend/src/test/resources/application.yaml`](../../backend/src/test/resources/application.yaml) có cấu hình mail, Redis, JWT và R2 cho test nhưng:

- Không cấu hình datasource test.
- Không có cơ chế bắt buộc kích hoạt profile `test`.
- Giá trị `jwt.expiration` hiện là `60G0000`, không phải số hợp lệ.
- Log của các `@SpringBootTest` hiển thị không có active profile và application name là `pccms`, không phải `pccms-test`.
- Trong lần chạy review, một số context kết nối tới PostgreSQL local thay vì Testcontainer.

Điều này làm giảm tính lặp lại và tạo nguy cơ test phụ thuộc hoặc tác động database ngoài phạm vi test.

---

## 3. Tổng quan bộ kiểm thử

### 3.1. Quy mô

| Chỉ số | Số lượng |
| --- | ---: |
| Tệp Java trong `src/test/java` | 71 |
| Lớp có kết quả Surefire trong `mvn verify` | 68 |
| Tệp CSV testcase | 35 |
| Dòng testcase CSV, không tính header | 329 |
| Lớp dùng Mockito Extension | 51 |
| Lớp controller dùng MockMvc standalone | 23 |
| Lớp security dùng Spring context và security filter chain | 3 |
| Lớp `*IT` không được verify mặc định chạy | 2 |

Ba tệp không tạo kết quả trong lần `mvn verify` gồm:

- `AbstractIntegrationTest`: base class, không chứa testcase.
- `MedicineRepositoryIT`: bị bỏ qua do quy tắc đặt tên.
- `PrescriptionConcurrencyIT`: bị bỏ qua do quy tắc đặt tên.

### 3.2. Kết quả theo module

Số liệu dưới đây lấy từ lần `mvn verify` chính, chưa bao gồm hai lớp `*IT`.

| Module | Lớp chạy | Testcase | Skipped | Line coverage |
| --- | ---: | ---: | ---: | ---: |
| appointment | 4 | 12 | 0 | 24,9% |
| billing | 8 | 50 | 0 | 72,6% |
| boarding | 4 | 8 | 0 | 30,7% |
| catalog | 2 | 17 | 0 | 51,6% |
| common | 1 | 1 | 0 | 91,8% |
| dev | 1 | 1 | 0 | Không tách riêng |
| filemedia | 2 | 3 | 0 | 85,2% |
| grooming | 2 | 11 | 0 | 46,7% |
| identity | 5 | 21 | 0 | 53,4% |
| medicalrecord | 7 | 25 | 0 | 44,2% |
| medicine | 4 | 46 | 0 | 30,0% |
| notification | 3 | 14 | 0 | 87,6% |
| pet | 2 | 18 | 0 | 35,3% |
| reception | 5 | 50 | 0 | 29,6% |
| report | 2 | 12 | 0 | 62,5% |
| room | 4 | 20 | 0 | 16,2% |
| schedule | 8 | 37 | 0 | 59,0% |
| user | 4 | 83 | 2 | 66,4% |

Các module cần ưu tiên bổ sung coverage là `room`, `appointment`, `reception`, `medicine`, `boarding` và `pet`.

---

## 4. Unit Test

### 4.1. Nội dung đã kiểm thử

Unit test tập trung vào:

- Luồng thành công và lỗi nghiệp vụ của service.
- Entity lifecycle và state transition.
- Mapping giữa entity và response.
- Validation tại service/controller.
- Error code của `BusinessException`.
- Tương tác repository/service bằng Mockito.
- Các testcase tham số hóa từ CSV.

Các module có unit test đáng kể gồm billing, user, schedule, reception, medical record, medicine, catalog, room, grooming, pet và notification.

### 4.2. Điểm tốt

- Tên test phần lớn mô tả rõ hành vi mong đợi.
- Nhiều test kiểm tra cả kết quả và interaction với dependency.
- CSV giúp truy vết testcase nghiệp vụ theo rule ID/case ID.
- Có kiểm tra success, validation, not found, invalid state và permission-related business rule.
- Không có testcase bị `@Disabled`.

### 4.3. Hạn chế

- Hai testcase trong `AdminAccountServiceTest` bị skip bằng assumption. Lý do là validation enum được chuyển sang controller test; quyết định này hợp lý theo tầng nhưng phải được truy vết rõ trong báo cáo.
- Ít nhất 30 invocation trong `AdminAccountControllerTest` và `MedicineCategoryControllerTest` thoát sớm với `return` khi dòng CSV không thuộc scenario của method. Surefire vẫn tính các invocation này là pass.
- Một số test parameterized xử lý nhiều nhánh trong cùng một method, làm khó xác định chính xác testcase nào thất bại và làm tăng độ phức tạp bảo trì.

### 4.4. Đánh giá

**Kết quả: Đạt có điều kiện.**

Logic nghiệp vụ chính có test, nhưng số lượng pass hiện tại bị phóng đại bởi các invocation không thực hiện kiểm tra. QA nên dùng số `429` như số invocation kỹ thuật, không coi đó là 429 testcase độc lập có assertion đầy đủ.

---

## 5. Integration Test

### 5.1. Nội dung đã kiểm thử

Các integration test được `mvn verify` chạy gồm:

- `PaymentIntegrationTest`: 4 testcase với PostgreSQL Testcontainer.
- `RefreshTokenRepositoryTest`: 2 testcase với PostgreSQL Testcontainer.
- `UserRepositoryTest`: 1 testcase với PostgreSQL Testcontainer.
- `RoomManagementSearchIntegrationTest`: 1 testcase dùng Spring context.
- Các security controller test dùng full Spring context nhưng mock service.

`PaymentIntegrationTest` kiểm tra được persistence, cập nhật trạng thái invoice và rollback khi lỗi.

### 5.2. Kết quả chạy riêng `*IT`

| Lớp | Testcase | Kết quả |
| --- | ---: | --- |
| `MedicineRepositoryIT` | 2 | Pass 2 |
| `PrescriptionConcurrencyIT` | 1 | Error 1 |

Lỗi của `PrescriptionConcurrencyIT`:

- Test tạo `Roles` chỉ có `code`.
- Schema hiện yêu cầu trường `roles.name` không null.
- Test dừng ở `setUp`, chưa chạy được logic cạnh tranh tồn kho.

### 5.3. Vấn đề chất lượng

- Maven chưa cấu hình Failsafe và không include `*IT`, nên build xanh dù integration test quan trọng đang lỗi.
- Test có tên `should_PreventConcurrentStockDeduction_When_UsingPessimisticLock` tạo 5 thread nhưng các thread không thực hiện transaction hoặc trừ kho. Assertion cuối chỉ xác nhận query khóa trả về entity.
- `PrescriptionConcurrencyIT` coi mọi exception chung là một lần thất bại cạnh tranh hợp lệ. Cách này có thể che lỗi kỹ thuật không liên quan đến thiếu tồn kho hoặc lock conflict.
- `doneLatch.await(5, TimeUnit.SECONDS)` không kiểm tra giá trị trả về, nên timeout có thể không được báo đúng nguyên nhân.
- Một số `@SpringBootTest` không dùng Testcontainer và phụ thuộc datasource local.

### 5.4. Đánh giá

**Kết quả: Không đạt đối với integration gate đầy đủ.**

Các test tích hợp đang được Maven chạy mặc định đều pass, nhưng pipeline bỏ sót hai lớp `*IT`, trong đó một lớp đang lỗi và một test đồng thời chưa kiểm tra đúng hành vi mô tả.

---

## 6. API/Controller Test

### 6.1. Nội dung đã kiểm thử

Controller test kiểm tra:

- HTTP status.
- Validation request.
- JSON response và error body.
- Mapping path/query/body.
- Exception handler.
- Một số luồng phân trang, tìm kiếm và cập nhật trạng thái.

### 6.2. Phân loại đúng mức kiểm thử

23 lớp controller sử dụng:

```java
MockMvcBuilders.standaloneSetup(...)
```

Đây là controller unit/slice-style test, không phải end-to-end API test vì không tự động tải:

- Toàn bộ Spring Security filter chain.
- Toàn bộ bean validation/configuration thực tế ngoài phần được đăng ký thủ công.
- Repository và database.
- Cấu hình serialization, filter, interceptor và infrastructure đầy đủ của ứng dụng.

Ba lớp security riêng dùng `webAppContextSetup(...).apply(springSecurity())`, phù hợp hơn để xác minh authorization.

### 6.3. Khoảng trống

- Không có Postman/Newman collection.
- Không có test gọi ứng dụng qua cổng HTTP thực tế.
- Không có contract test/OpenAPI validation tự động.
- Tài liệu hiện mô tả một số test là `@WebMvcTest`, nhưng mã nguồn không có lớp nào sử dụng annotation này.

### 6.4. Đánh giá

**Kết quả: Đạt có điều kiện ở tầng controller; chưa đủ bằng chứng cho API end-to-end.**

---

## 7. Security Test

### 7.1. Nội dung đã kiểm thử

Ba nhóm security controller test bao phủ 24 testcase tại các module:

- Billing.
- Medicine.
- Room.

Các testcase kiểm tra:

- Không xác thực trả `401`.
- Có xác thực nhưng thiếu authority trả `403`.
- Có authority phù hợp được phép truy cập.
- Service không bị gọi khi request bị từ chối.

Ngoài ra có:

- `JwtAuthenticationFilterTest`: token hợp lệ và tình huống Redis blacklist không khả dụng.
- `RateLimitFilterTest`: xác minh request vẫn được phép khi Redis rate-limit backend không khả dụng.
- Một số test nghiệp vụ IDOR ở pet và personal schedule.

### 7.2. Khoảng trống

Chưa có bằng chứng tự động cho:

- JWT sai chữ ký, malformed, hết hạn hoặc bị blacklist.
- Refresh token hết hạn/replay ở mức HTTP.
- SQL Injection và XSS payload qua API thực tế.
- Kiểm tra stack trace/thông tin nội bộ trong response trên toàn hệ thống.
- Kiểm tra object-level authorization xuyên suốt controller-service-database.
- DAST bằng OWASP ZAP.
- Kiểm tra rate-limit đạt ngưỡng, vượt ngưỡng và reset cửa sổ.

### 7.3. Đánh giá

**Kết quả: Đạt một phần đối với authorization; chưa đạt phạm vi Security Test toàn diện.**

---

## 8. Performance Test

Repository chưa có:

- Kịch bản k6/JMeter.
- Chỉ tiêu latency p95/p99.
- Chỉ tiêu throughput.
- Ngưỡng error rate.
- Load, stress, spike hoặc soak test.
- Baseline tài nguyên CPU, RAM, connection pool hoặc database.

**Kết quả: Chưa thực hiện.**

Trước khi thực hiện, Product/QA/Backend cần thống nhất workload, dữ liệu test và tiêu chí chấp nhận. Không nên tự kết luận hệ thống đáp ứng tải chỉ dựa trên thời gian chạy unit/integration test.

---

## 9. Coverage

JaCoCo phân tích 301 lớp:

| Metric | Covered | Total | Tỷ lệ |
| --- | ---: | ---: | ---: |
| Instruction | 12.040 | 25.768 | 46,72% |
| Branch | 543 | 1.698 | 31,98% |
| Line | 2.302 | 5.033 | 45,74% |
| Complexity | 669 | 2.067 | 32,37% |
| Method | 534 | 1.210 | 44,13% |

Nhận xét:

- Coverage hiện ở mức trung bình thấp và không đồng đều giữa các module.
- Branch coverage thấp cho thấy nhiều nhánh lỗi, điều kiện biên hoặc state transition chưa được thực thi.
- JaCoCo chỉ tạo report; chưa có rule `jacoco:check` để fail build khi coverage giảm.
- Coverage hiện tại không bao gồm hai lớp `*IT` trong lần `mvn verify`.

Report sinh tại:

- `backend/target/site/jacoco/index.html`
- `backend/target/site/jacoco/jacoco.csv`
- `backend/target/site/jacoco/jacoco.xml`

---

## 10. Phát hiện và rủi ro

| ID | Mức độ | Phát hiện | Ảnh hưởng |
| --- | --- | --- | --- |
| TEST-001 | High | CI backend chỉ chạy `clean compile`, không chạy test/verify | PR có test fail vẫn có thể qua CI |
| TEST-002 | High | Hai lớp `*IT` không được Maven verify chạy; một lớp đang error | Build xanh giả, bỏ sót lỗi integration/concurrency |
| TEST-003 | High | Một số Spring context test dùng datasource local, không có profile test bắt buộc | Test không lặp lại, có nguy cơ đọc/ghi DB ngoài test |
| TEST-004 | High | Test concurrency của Medicine tạo thread nhưng không thực hiện thao tác đồng thời | Không chứng minh được pessimistic lock hoặc chống oversell |
| TEST-005 | Medium | Ít nhất 30 parameterized invocation pass do thoát sớm, không assertion | Số lượng pass bị phóng đại |
| TEST-006 | Medium | Line/branch coverage 45,74%/31,98%, không có quality gate | Regression ở nhánh ít dùng khó bị phát hiện |
| TEST-007 | Medium | Security test mới tập trung authority, thiếu invalid JWT, DAST và input attack | Chưa đủ cơ sở đánh giá rủi ro OWASP |
| TEST-008 | Medium | Chưa có Performance Test | Không có dữ liệu về latency, throughput và capacity |
| TEST-009 | Medium | Tài liệu testcase mô tả `@WebMvcTest`, random port, active profile và cleanup khác mã thực tế | QA có thể đánh giá sai mức độ tích hợp |
| TEST-010 | Low | Mockito cảnh báo self-attach agent trên Java 25; Testcontainers có dấu hiệu lệch version module/core | Rủi ro tương thích ở lần nâng JDK/dependency sau |

### 10.1. Ghi nhận lỗi theo mẫu QA

| Mã lỗi | Chức năng | Mô tả | Mức độ | Trạng thái |
| --- | --- | --- | --- | --- |
| BUG-TEST-001 | CI backend | CI không chạy test | High | Open |
| BUG-TEST-002 | Integration pipeline | `*IT` không được chạy bởi verify | High | Open |
| BUG-TEST-003 | Prescription concurrency | Test lỗi do thiếu `roles.name` trong seed data | High | Open |
| BUG-TEST-004 | Medicine concurrency | Thread không thực hiện thao tác concurrent | High | Open |
| BUG-TEST-005 | Test isolation | SpringBootTest phụ thuộc datasource local | High | Open |
| BUG-TEST-006 | Test reporting | Parameterized test ghi nhận pass cho dòng không được kiểm tra | Medium | Open |

---

## 11. Khuyến nghị và tiêu chí đóng

### 11.1. P0 - Trước khi QA ký duyệt

1. Đổi CI backend từ `clean compile` sang `clean verify`.
2. Cấu hình Maven Failsafe cho `**/*IT.java`, hoặc đổi tên test để Surefire chạy nhất quán.
3. Sửa `PrescriptionConcurrencyIT` để seed đầy đủ role và xác minh đúng loại exception.
4. Viết lại `MedicineRepositoryIT` để các thread thực sự mở transaction, giữ lock và cập nhật tồn kho.
5. Bắt buộc profile test và datasource Testcontainer cho mọi `@SpringBootTest`; không dùng database local.
6. Loại bỏ các invocation no-op bằng cách tách CSV theo action/scenario hoặc dùng `@MethodSource` lọc dữ liệu trước khi tạo invocation.

### 11.2. P1 - Nâng chất lượng regression

1. Thống nhất coverage gate với QA và tăng dần theo module, ưu tiên branch coverage.
2. Bổ sung API test chạy qua HTTP thật trên random port.
3. Bổ sung invalid/expired/revoked JWT và refresh-token lifecycle.
4. Bổ sung object-level authorization cho các tài nguyên owner/staff.
5. Thêm SQLi/XSS/error disclosure test và OWASP ZAP baseline scan.
6. Đồng bộ tài liệu trong `docs/testing` với annotation và hạ tầng test thực tế.
7. Chuẩn hóa version Testcontainers theo dependency management của Spring Boot.

### 11.3. P2 - Hiệu năng và vận hành

1. Xây dựng k6/JMeter smoke test cho login, search, appointment, invoice và payment.
2. Thống nhất SLA/SLO cho p95, p99, throughput và error rate.
3. Chạy load test trên staging có dữ liệu gần production.
4. Lưu kết quả và xu hướng hiệu năng như artifact của pipeline.

---

## 12. Kết luận

Bộ test backend PCCMS đã có nền tảng tốt cho unit regression và kiểm tra controller, với dữ liệu testcase được tổ chức theo CSV và bao phủ nhiều module nghiệp vụ. Lần chạy regression mặc định đạt 427 pass, không có failure/error và tạo được báo cáo JaCoCo.

Tuy nhiên, trạng thái tổng thể chưa thể đánh giá là “đạt hoàn toàn” vì integration test quan trọng đang nằm ngoài pipeline, một test concurrency đang lỗi, một test concurrency khác không kiểm tra hành vi mô tả, CI không chạy test, môi trường SpringBootTest chưa cô lập và chưa có performance/DAST.

**Kết luận đề xuất cho quản lý và QA:**

- Unit Test: **Đạt có điều kiện**.
- Integration Test: **Không đạt gate đầy đủ**.
- API/Controller Test: **Đạt có điều kiện, chưa phải end-to-end**.
- Security Test: **Đạt một phần**.
- Performance Test: **Chưa thực hiện**.
- Tổng thể: **Chưa sẵn sàng ký duyệt QA toàn diện cho phát hành cho đến khi hoàn thành các mục P0**.

---

## 13. Tài liệu và bằng chứng liên quan

- [Maven configuration](../../backend/pom.xml)
- [Backend test source](../../backend/src/test)
- [Test configuration](../../backend/src/test/resources/application.yaml)
- [CI workflow](../../.github/workflows/ci.yml)
- [Integration test specification](integration-test-cases.md)
- [Security test specification](security-test-cases.md)
- [Billing API test specification](billing-api-test-cases.md)
- [Billing payment test specification](billing-payment-test-cases.md)
- [Medicine category API test specification](medicine-category-api-test-cases.md)
- [Notification test specification](notification-test-cases.md)
- [Reception API test specification](reception-api-test-cases.md)
