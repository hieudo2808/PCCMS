# Integration Test Case Specification

## 1. Overview and Infrastructure

This document outlines the P0 and P1 integration test cases for critical transactional flows within the PCCMS backend, primarily focusing on Billing and Reception.

### Integration Infrastructure Strategy
- **Shared Base Class**: `AbstractIntegrationTest`
- **Annotations**: `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`, `@Testcontainers`, `@ActiveProfiles("test")`
- **Container**: `static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");`
  - The `static` keyword ensures the container starts once and is shared across all test classes extending the base class, significantly reducing test suite runtime.
- **Dynamic Properties**: Use `@DynamicPropertySource` in the base class to configure `spring.datasource.url`, `username`, and `password` to point to the running Testcontainer.
- **Cleanup Strategy**: Use `@Transactional` on test methods/classes where possible to auto-rollback after each test, or implement a `@AfterEach` method in the base class to truncate affected tables if explicit transactions are committed.
- **Seed Data Strategy**:
  - Prefer `@Sql` scripts for stable prerequisites (e.g., populating `service_catalog`, users, pets).
  - Use code builders/factories only for scenario-specific entities (e.g., a specific `Appointment` or `Invoice`).
  - Do not use CSV files for integration tests.
  - Do not rely on external service calls (use WireMock if absolutely necessary).

---

## 2. Billing Integration Tests

**Target Integration Area**: Billing Payment persistence/state-transition flow.
**Target Test Class**: `PaymentIntegrationTest`

### INT-BILL-001: Record Payment Success
* **Module**: Billing
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `PaymentService`, `InvoiceRepository`, `PaymentRepository`, Database
* **Why this is not covered by Unit/API/Security tests**: Verifies exact database-level locking and persistent state transition mapping that Mockito cannot simulate.
* **Required Seed Data**: Existing `Invoice` in `PENDING` state.
* **Transaction Boundary**: Starts at `PaymentService.createPayment()`.
* **Expected Persisted Database State**: 
  - New `Payment` record inserted with correct foreign key to `Invoice`.
  - `Invoice` status transitioned from `PENDING` to `PAID`.
* **Expected Rollback/Exception**: N/A
* **Priority**: P0

### INT-BILL-002: Invalid Payment Fails Without Marking Invoice Paid
* **Module**: Billing
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `PaymentService`, `InvoiceRepository`, Database
* **Why this is not covered by Unit/API/Security tests**: Verifies `@Transactional` rollback on validation/business rule failure.
* **Required Seed Data**: Existing `Invoice` in `PENDING` state.
* **Transaction Boundary**: Starts at `PaymentService.createPayment()`.
* **Expected Persisted Database State**: 
  - No `Payment` record is inserted.
  - `Invoice` status remains `PENDING`.
* **Expected Rollback/Exception**: Throws `BusinessException` / Rollback occurs.
* **Priority**: P0

### INT-BILL-003: Duplicate/Conflicting Payment Rejected
* **Module**: Billing
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `PaymentService`, `InvoiceRepository`, Database Lock
* **Why this is not covered by Unit/API/Security tests**: Verifies database constraints (e.g., optimistic/pessimistic locking) when two threads/calls attempt to pay the same invoice simultaneously or attempting to pay an already `PAID` invoice.
* **Required Seed Data**: Existing `Invoice` in `PAID` state.
* **Transaction Boundary**: Starts at `PaymentService.createPayment()`.
* **Expected Persisted Database State**: 
  - No new `Payment` record is inserted.
  - `Invoice` status remains `PAID`.
* **Expected Rollback/Exception**: Throws conflict `BusinessException` (e.g., `Invoice already paid`).
* **Priority**: P0

### INT-BILL-004: Invoice Not Found Rolls Back
* **Module**: Billing
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `PaymentService`, Database
* **Why this is not covered by Unit/API/Security tests**: Verifies robust exception propagation from persistence layer.
* **Required Seed Data**: None (Non-existent Invoice ID).
* **Transaction Boundary**: Starts at `PaymentService.createPayment()`.
* **Expected Persisted Database State**: Clean (no dirty `Payment` records).
* **Expected Rollback/Exception**: `ERR_BILL_001_INVOICE_NOT_FOUND`
* **Priority**: P0

---

## 3. Reception Integration Tests

**Target Integration Area**: Appointment Reception quick check-in multi-table transactional flow.
**Target Test Class**: `AppointmentReceptionIntegrationTest`

### INT-REC-001: Quick Appointment Creation Persists All Entities
* **Module**: Reception
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `AppointmentReceptionService`, underlying Repositories, Database
* **Why this is not covered by Unit/API/Security tests**: Validates multi-table transaction insertion constraints (Order, Pet, Appointment).
* **Required Seed Data**: Owner User, Service Catalog items.
* **Transaction Boundary**: Starts at `AppointmentReceptionService.quickCreateAndReceive()`.
* **Expected Persisted Database State**: 
  - Pet record created/linked.
  - Service Order record created.
  - Appointment record created with `CHECKED_IN` status.
* **Expected Rollback/Exception**: N/A
* **Priority**: P0

### INT-REC-002: Receiving Non-existent Appointment Leaves No Artifacts
* **Module**: Reception
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `AppointmentReceptionService`, Database
* **Why this is not covered by Unit/API/Security tests**: Ensures no side-effect records are created before failure.
* **Required Seed Data**: None (Non-existent Appointment ID).
* **Transaction Boundary**: Starts at `AppointmentReceptionService.receive()`.
* **Expected Persisted Database State**: Unchanged.
* **Expected Rollback/Exception**: `ERR_REC_001_APPOINTMENT_NOT_FOUND`
* **Priority**: P0

### INT-REC-003: Invalid Transition Fails Without Corruption
* **Module**: Reception
* **Test Type**: Integration Test
* **Tools**: Spring Boot Test, Testcontainers, PostgreSQLContainer
* **Components Under Test**: `AppointmentReceptionService`, Database
* **Why this is not covered by Unit/API/Security tests**: Validates safe state handling against DB consistency.
* **Required Seed Data**: Appointment in `CANCELLED` state.
* **Transaction Boundary**: Starts at `AppointmentReceptionService.receive()`.
* **Expected Persisted Database State**: 
  - Appointment remains `CANCELLED`.
* **Expected Rollback/Exception**: `ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE`
* **Priority**: P0

---

## 4. Boarding Integration Tests (Documentation Only)

**Target Integration Area**: Boarding care log persistence.
**Target Test Class**: `BoardingCareLogIntegrationTest`

### INT-BOARD-001: Save Care Log Success
* **Module**: Reception (Boarding)
* **Test Type**: Integration Test
* **Components Under Test**: `BoardingCareLogService`, Database
* **Required Seed Data**: Active `BoardingSession`, `Pet`.
* **Expected Persisted Database State**: Log saved and linked to session.
* **Priority**: P1

### INT-BOARD-002: Save Care Log Invalid Foreign Key
* **Module**: Reception (Boarding)
* **Test Type**: Integration Test
* **Components Under Test**: `BoardingCareLogService`, Database Constraints
* **Required Seed Data**: None.
* **Expected Rollback/Exception**: `DataIntegrityViolationException` or BusinessException.
* **Priority**: P1

---

## 5. Medicine Integration Tests (Documentation Only)

**Target Integration Area**: Medicine category conflict persistence.
**Target Test Class**: `MedicineCategoryIntegrationTest`

### INT-MED-001: Category Unique Name Conflict
* **Module**: Medicine
* **Test Type**: Integration Test
* **Components Under Test**: `MedicineCategoryService`, Database Unique Constraint
* **Required Seed Data**: Category "ANTIBIOTIC".
* **Expected Rollback/Exception**: Native `DataIntegrityViolationException` propagated or translated correctly.
* **Priority**: P1

### INT-MED-002: Delete In-Use Category Foreign Key Conflict
* **Module**: Medicine
* **Test Type**: Integration Test
* **Components Under Test**: `MedicineCategoryService`, Database FK Constraint
* **Required Seed Data**: Category with linked Medicine.
* **Expected Rollback/Exception**: Native constraint violation translated to `ERR_MED_010_CATEGORY_IN_USE`.
* **Priority**: P1
