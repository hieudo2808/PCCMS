---
trigger: always_on
---

# [TESTING-CONSTITUTION] MANDATORY TESTING RULES FOR PCCMS

> **MỤC ĐÍCH TỐI THƯỢNG:** Mọi mã nguồn được sinh ra bởi AI Agent phải đi kèm chiến lược kiểm thử có hệ thống theo tiêu chuẩn ITSS, ISTQB và Enterprise Software Quality Engineering. Mục tiêu KHÔNG PHẢI đạt Coverage cao một cách hình thức, mà phải CHỨNG MINH được tính đúng đắn của nghiệp vụ, khả năng phát hiện lỗi và khả năng bảo trì lâu dài.

## 1. RULE 1 – TEST DESIGN FIRST (BẮT BUỘC)

AI Agent **TUYỆT ĐỐI KHÔNG ĐƯỢC** viết Unit Test trực tiếp từ source code (Implementation) trước khi thực hiện phân tích đặc tả và thiết kế testcase. Bắt buộc thực hiện quy trình sau:
`Requirement` → `Business Rule Extraction` → `Test Condition Identification` → `Test Case Design` → `Test Dataset Creation` → `Automated Test Implementation`.

AI Agent phải xác định rõ ra nháp (hoặc log) trước khi code:

1. Business Rules
2. Input Conditions
3. Validation Rules
4. State Changes
5. Expected Outputs
6. Error Conditions

## 2. RULE 12 – FORBIDDEN PRACTICES (CÁC HÀNH VI BỊ NGHIÊM CẤM)

AI Agent **TUYỆT ĐỐI CẤM** thực hiện các hành vi sau. Vi phạm đồng nghĩa với việc phá hoại dự án:

- [x] CẤM viết testcase chỉ để tăng coverage.
- [x] CẤM viết testcase dựa trên implementation hiện tại thay vì requirement.
- [x] CẤM hardcode dữ liệu test trong source code.
- [x] CẤM copy/paste nhiều testcase giống nhau (phải dùng ParameterizedTest).
- [x] CẤM bỏ qua Boundary Cases, Invalid Cases, Error Scenarios.
- [x] CẤM viết Unit Test phụ thuộc Database, Network, API ngoài, Thời gian thực, hoặc File System.

## 3. SUCCESS CRITERIA (TIÊU CHÍ HOÀN THÀNH)

Một tính năng chỉ được AI Agent đánh dấu là "Hoàn thành kiểm thử" khi:

- [ ] Business Rules được xác định rõ ràng.
- [ ] Black Box Test được thiết kế (BVA, EP, Decision Table...).
- [ ] Branch Coverage đạt 100% C1 (đối với Service).
- [ ] Test Data nằm hoàn toàn trong file CSV.
- [ ] Test Logic chỉ implement một lần (Single Source of Truth).
- [ ] Requirement Traceability tồn tại (Có mapping với Rule ID).
- [ ] Unit Test, Integration Test và Controller Test được tách biệt hoàn toàn.
