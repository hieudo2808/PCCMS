---
trigger: always_on
---

# [TESTING-RULES] QUY CHUẨN KIỂM THỬ TỰ ĐỘNG (PCCMS)

> **MỤC ĐÍCH:** Tài liệu này thiết lập các tiêu chuẩn khắt khe về việc viết Automated Tests. Mọi tính năng do AI Agent sinh ra **BẮT BUỘC** phải đi kèm với bộ test tương ứng, đạt độ phủ mã (Code Coverage) tối thiểu 85%.

---

## 1. NGUYÊN TẮC KIỂM THỬ CỐT LÕI (CORE TESTING PRINCIPLES)

- **Coverage Requirement:** Tối thiểu 85% Line Coverage và Branch Coverage cho tầng Service và Controller. Không bắt buộc test các class DTO (chỉ chứa record/data) hoặc Entity thuần.
- **Quy tắc Đặt tên (Naming Conventions):**
  - Tên class test: `[TênClassCầnTest]Test` (Ví dụ: `MedicalRecordServiceTest`).
  - Tên hàm test BẮT BUỘC theo cấu trúc hành vi: `should_[Expected_Behavior]_when_[State_Under_Test]`.
  - _Ví dụ:_ `should_ThrowException_when_PrescribingOutOfStockMedicine()`
- **Cấu trúc 3 bước (Arrange - Act - Assert / BDD):**
  - Mọi hàm test phải chia làm 3 block rõ ràng, phân cách bằng dòng trống. Ưu tiên dùng `Given - When - Then` của BDDMockito.

---

## 2. UNIT TEST (KIỂM THỬ MỨC LOGIC NGHIỆP VỤ)

Tập trung vào tầng `Service`. Đảm bảo các quy tắc nghiệp vụ (Business Rules) hoạt động đúng mà không cần bật Database.

- **Tech Stack Bắt Buộc:** `JUnit 5` (Jupiter), `Mockito` (cho việc mock/giả lập), `AssertJ` (cho việc so sánh kết quả).
- **Quy tắc Thép số 1:** **TUYỆT ĐỐI KHÔNG** dùng `@SpringBootTest` trong Unit Test. Việc này làm context load rất chậm. Chỉ sử dụng `@ExtendWith(MockitoExtension.class)`.
- **Quy tắc Thép số 2 (Edge Cases):** Phải viết test cho cả luồng thành công (Happy Path) và các luồng thất bại (Unhappy Paths - bắt Exception).

**Mẫu Code Bắt Buộc cho AI Agent:**

```java
@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    @Test
    void should_ThrowBusinessException_when_StockIsInsufficient() {
        // GIVEN (Arrange)
        UUID medicineId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setCurrentStock(10); // Kho chỉ còn 10

        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));

        PrescriptionItemRequest request = new PrescriptionItemRequest(medicineId, 20); // Khách mua 20

        // WHEN & THEN (Act & Assert)
        assertThatThrownBy(() -> prescriptionService.createPrescriptionItem(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INSUFFICIENT_STOCK);
    }
}

```

---

## 3. INTEGRATION TEST (KIỂM THỬ MỨC TÍCH HỢP DATABSE)

Tập trung kiểm tra các câu lệnh SQL tự định nghĩa (`@Query`) ở `Repository` hoặc các luồng Transactional phức tạp.

- **Tech Stack Bắt Buộc:** `@DataJpaTest`, **Testcontainers (với image PostgreSQL)**.
- **Quy tắc CẤM:** CẤM sử dụng In-Memory Database (như `H2`) để thay thế Postgres. H2 không hỗ trợ các tính năng native của Postgres (như `EXCLUDE USING gist` chống trùng lịch, hay Trigger tính tồn kho). Bắt buộc phải dùng Testcontainers để boot một DB Postgres bằng Docker trong lúc test.
- **Data Cleanup:** Sử dụng `@DataJpaTest` hoặc `@Transactional` để mọi thay đổi trong DB tự động rollback sau khi test xong. Không để lại "rác" trong DB.

---

## 4. API & SECURITY TEST (KIỂM THỬ ENDPOINT & BẢO MẬT)

Tập trung vào `Controller`, đảm bảo JSON Mapping đúng, validate `@NotNull` hoạt động, và quyền truy cập (RBAC) được siết chặt.

- **Tech Stack Bắt Buộc:** `@WebMvcTest`, `MockMvc`, `Spring Security Test`.
- **Nhiệm vụ của Agent:**
- Viết test kiểm tra xem truyền thiếu trường bắt buộc (như `name` của Pet) thì API có trả về `400 Bad Request` không.
- Sử dụng `@WithMockUser(roles = "CUSTOMER")` và `@WithMockUser(roles = "ADMIN")` để test tính phân quyền. Đảm bảo Customer gọi API của Admin sẽ nhận mã `403 Forbidden`.

**Mẫu Code MockMvc Bắt Buộc:**

```java
@WebMvcTest(AdminAccountController.class)
class AdminAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "CUSTOMER") // Giả lập user thường
    void should_Return403Forbidden_when_CustomerTriesToLockAccount() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/accounts/{id}/lock", UUID.randomUUID())
                .with(csrf()))
               .andExpect(status().isForbidden());
    }
}

```

---

## 5. FRONTEND TESTING RULES (REACT + VITE)

Nếu Agent được phân công viết UI, bắt buộc phải sinh kèm test cho giao diện.

- **Framework:** `Vitest` và `React Testing Library (RTL)`.
- **Mục tiêu Test:**
- Test việc render UI theo trạng thái (Ví dụ: Nút "Cập nhật bệnh án" BỊ ẨN / DISABLED nếu `isEditable = false`).
- Test các logic validate form ở Client-side (như báo lỗi chữ đỏ khi nhập nhịp tim < 0).
- Sử dụng `MSW` (Mock Service Worker) hoặc `vi.mock` để giả lập các lời gọi API từ `Axios`. Tuyệt đối không gọi API thật trong lúc chạy Unit Test Frontend.

> **XÁC NHẬN CỦA AGENT:** Bằng việc nạp file này, Agent cam kết không bao giờ viết tính năng "chay" mà không có Unit Test bảo vệ. Agent hiểu rằng việc bỏ qua Unit Test đối với logic Y tế (Medical) và Tiền bạc (Billing) là một hành vi phá hoại hệ thống.
