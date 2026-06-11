# Billing API Test Case Specification

## Module Overview
This document specifies the P0/P1 testing requirements for `InvoiceController`, `PaymentController`, and `OwnerPaymentController`.
The tests focus on Controller Slice testing (`@WebMvcTest`) focusing on HTTP Status, Request Validation (`@Valid`), Security Constraints (`@PreAuthorize`), and Response mapping.

---

## A. InvoiceController

### TC-BILL-API-INV-001: List My Invoices
- **Test type:** API Test & Security Test
- **Tool:** MockMvc
- **Endpoint:** `GET /v1/invoices/my`
- **Security:** Requires `INVOICE_READ`
- **Objective:** Verify authenticated users with `INVOICE_READ` can fetch their invoices.
- **Expected result:**
  - With `INVOICE_READ` role: Returns `200 OK` and `PageResponse` JSON structure.
  - With `INVOICE_MANAGE` role: Returns `403 Forbidden`.
  - Unauthenticated: Returns `401 Unauthorized`.

### TC-BILL-API-INV-002: List All Invoices (Staff)
- **Test type:** API Test & Security Test
- **Tool:** MockMvc
- **Endpoint:** `GET /v1/invoices`
- **Security:** Requires `INVOICE_MANAGE`
- **Objective:** Verify staff users can fetch all invoices.
- **Expected result:**
  - With `INVOICE_MANAGE` role: Returns `200 OK` and `PageResponse` JSON structure.
  - With `INVOICE_READ` role: Returns `403 Forbidden`.

### TC-BILL-API-INV-003: Get Invoice Details
- **Test type:** API Test
- **Tool:** MockMvc
- **Endpoint:** `GET /v1/invoices/{invoiceId}`
- **Security:** Requires `INVOICE_READ` OR `INVOICE_MANAGE`
- **Objective:** Verify anyone with either authority can view an invoice's details.
- **Expected result:** Returns `200 OK` and `InvoiceResponse` JSON.

---

## B. PaymentController

### TC-BILL-API-PAY-001: Record Payment
- **Test type:** API Test
- **Tool:** MockMvc
- **Endpoint:** `POST /v1/payments`
- **Security:** Requires `INVOICE_MANAGE`
- **Objective:** Verify a staff user can record a direct payment successfully.
- **Input data:** Valid `RecordPaymentRequest` JSON.
- **Expected result:** Returns `201 CREATED`.

### TC-BILL-API-PAY-002: Record Payment - Validation Failure
- **Test type:** Validation Test
- **Tool:** MockMvc
- **Endpoint:** `POST /v1/payments`
- **Objective:** Ensure `@Valid` blocks missing or invalid request fields.
- **Input data:** Invalid JSON (missing `amountVnd` or `methodCode`).
- **Expected result:** Returns `400 BAD REQUEST`.

### TC-BILL-API-PAY-003: Update Payment Status
- **Test type:** API Test
- **Tool:** MockMvc
- **Endpoint:** `PATCH /v1/payments/{paymentId}/status`
- **Security:** Requires `INVOICE_MANAGE`
- **Objective:** Verify payment status updates succeed.
- **Input data:** Valid `PaymentStatusUpdateRequest` JSON.
- **Expected result:** Returns `200 OK`.

### TC-BILL-API-PAY-004: Update Payment Status - Validation Failure
- **Test type:** Validation Test
- **Tool:** MockMvc
- **Endpoint:** `PATCH /v1/payments/{paymentId}/status`
- **Objective:** Ensure `@Valid` blocks missing status code.
- **Input data:** JSON missing `statusCode`.
- **Expected result:** Returns `400 BAD REQUEST`.

### TC-BILL-API-PAY-005: Create Owner Payment Request (Legacy path)
- **Test type:** API Test & Security Test
- **Tool:** MockMvc
- **Endpoint:** `POST /v1/payments/me/invoices/{invoiceId}/payment-requests`
- **Security:** Requires `INVOICE_READ`
- **Objective:** Verify owner payment requests via the legacy path succeed.
- **Input data:** Valid `OwnerPaymentRequest` JSON.
- **Expected result:** Returns `201 CREATED`.

---

## C. OwnerPaymentController

### TC-BILL-API-OWN-001: Create Owner Payment Request
- **Test type:** API Test
- **Tool:** MockMvc
- **Endpoint:** `POST /v1/me/invoices/{invoiceId}/payment-requests`
- **Security:** Requires `INVOICE_READ`
- **Objective:** Verify standard owner payment requests succeed.
- **Input data:** Valid `OwnerPaymentRequest` JSON.
- **Expected result:** Returns `201 CREATED`.

### TC-BILL-API-OWN-002: Create Owner Payment Request - Validation Failure
- **Test type:** Validation Test
- **Tool:** MockMvc
- **Endpoint:** `POST /v1/me/invoices/{invoiceId}/payment-requests`
- **Objective:** Ensure `@Valid` blocks invalid inputs.
- **Input data:** JSON missing `amountVnd` or `methodCode`.
- **Expected result:** Returns `400 BAD REQUEST`.
