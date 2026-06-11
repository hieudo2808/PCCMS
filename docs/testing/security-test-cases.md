# Security Test Case Specification

This document details the P0 security test cases for high-risk endpoints in the Billing, Medicine, and Room modules.

**Important Notes:**
- Existing standalone MockMvc controller tests are API tests, not security tests.
- Security tests must use Spring Security Test framework (`@WithMockUser`, etc.).
- Security tests must ensure that the Spring Security filter chain and method security (`@EnableMethodSecurity`) are active.
- Do not rely on calling controller methods directly. MockMvc must perform actual HTTP routing to trigger `@PreAuthorize`.
- Service beans must be mocked; they should not be called if the request is unauthorized (401) or forbidden (403).

## 1. Billing & Payment Protected APIs

**Target Controllers:** `InvoiceController`, `PaymentController`, `OwnerPaymentController`
**Automation Target Class:** `BillingSecurityControllerTest`

### Test Case: SEC-BILL-001
- **Endpoint:** `/v1/invoices`
- **HTTP Method:** `GET`
- **Protected Operation:** List all invoices (Manage)
- **Required Authority:** `INVOICE_MANAGE`
- **Contexts:**
  - **SEC-BILL-001.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-BILL-001.2:** Authenticated (without `INVOICE_MANAGE`) -> `403 Forbidden` (Service not called)
  - **SEC-BILL-001.3:** Authenticated (with `INVOICE_MANAGE`) -> `2xx Success` (Service called)
- **Priority:** P0

### Test Case: SEC-BILL-002
- **Endpoint:** `/v1/invoices/my`
- **HTTP Method:** `GET`
- **Protected Operation:** List my invoices (Owner)
- **Required Authority:** `INVOICE_READ`
- **Contexts:**
  - **SEC-BILL-002.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-BILL-002.2:** Authenticated (without `INVOICE_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-BILL-002.3:** Authenticated (with `INVOICE_READ`) -> `2xx Success` (Service called)
- **Priority:** P0

### Test Case: SEC-BILL-003
- **Endpoint:** `/v1/payments`
- **HTTP Method:** `POST`
- **Protected Operation:** Record a payment (Manage)
- **Required Authority:** `INVOICE_MANAGE`
- **Contexts:**
  - **SEC-BILL-003.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-BILL-003.2:** Authenticated (without `INVOICE_MANAGE`, e.g., with only `INVOICE_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-BILL-003.3:** Authenticated (with `INVOICE_MANAGE`) -> `2xx Success` (Service called)
- **Priority:** P0

### Test Case: SEC-BILL-004
- **Endpoint:** `/v1/me/invoices/{invoiceId}/payment-requests`
- **HTTP Method:** `POST`
- **Protected Operation:** Submit proof of payment (Owner)
- **Required Authority:** `INVOICE_READ`
- **Contexts:**
  - **SEC-BILL-004.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-BILL-004.2:** Authenticated (without `INVOICE_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-BILL-004.3:** Authenticated (with `INVOICE_READ`) -> `2xx Success` (Service called)
- **Priority:** P0

## 2. Medicine Protected APIs

**Target Controllers:** `MedicineController`, `MedicineCategoryController`
**Automation Target Class:** `MedicineSecurityControllerTest`

### Test Case: SEC-MED-001
- **Endpoint:** `/v1/medicines`
- **HTTP Method:** `POST`
- **Protected Operation:** Create medicine (Manage)
- **Required Authority:** `MEDICINE_MANAGE`
- **Contexts:**
  - **SEC-MED-001.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-MED-001.2:** Authenticated (with only `MEDICINE_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-MED-001.3:** Authenticated (with `MEDICINE_MANAGE`) -> `2xx Success` (Service called)
- **Priority:** P0

### Test Case: SEC-MED-002
- **Endpoint:** `/v1/medicine-categories/{id}`
- **HTTP Method:** `GET`
- **Protected Operation:** Read medicine category (Read)
- **Required Authority:** `MEDICINE_MANAGE` OR `PRESCRIPTION_CREATE`
- **Contexts:**
  - **SEC-MED-002.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-MED-002.2:** Authenticated (without any required authority) -> `403 Forbidden` (Service not called)
  - **SEC-MED-002.3:** Authenticated (with `PRESCRIPTION_CREATE`) -> `2xx Success` (Service called)
- **Priority:** P1

## 3. Room Protected APIs

**Target Controllers:** `RoomManagementController`, `CatalogRoomController`, `CatalogRoomTypeController`
**Automation Target Class:** `RoomSecurityControllerTest`

### Test Case: SEC-ROOM-001
- **Endpoint:** `/v1/rooms` (or `/v1/catalog-rooms`)
- **HTTP Method:** `POST`
- **Protected Operation:** Create room (Manage)
- **Required Authority:** `ROOM_MANAGE`
- **Contexts:**
  - **SEC-ROOM-001.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-ROOM-001.2:** Authenticated (with only `ROOM_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-ROOM-001.3:** Authenticated (with `ROOM_MANAGE`) -> `2xx Success` (Service called)
- **Priority:** P0

### Test Case: SEC-ROOM-002
- **Endpoint:** `/v1/catalog-rooms/{id}`
- **HTTP Method:** `GET`
- **Protected Operation:** Read room info (Read)
- **Required Authority:** `ROOM_READ`
- **Contexts:**
  - **SEC-ROOM-002.1:** Unauthenticated -> `401 Unauthorized` (Service not called)
  - **SEC-ROOM-002.2:** Authenticated (without `ROOM_READ`) -> `403 Forbidden` (Service not called)
  - **SEC-ROOM-002.3:** Authenticated (with `ROOM_READ`) -> `2xx Success` (Service called)
- **Priority:** P0
