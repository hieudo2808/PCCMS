# Billing/Payment Test Case Specification

## Module Overview
This document specifies the P0 testing requirements for the `PaymentService` and `InvoiceMapper` within the Billing/Payment subsystem.

## A. PaymentService.recordPayment()

### TC-BILL-PAY-001: Successful Full Payment
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify that submitting a payment equal to the total invoice amount correctly marks the invoice as `PAID`.
- **Preconditions:** Valid active invoice with `UNPAID` status, authenticated staff user.
- **Input data:** `amount = 1000`, `total_amount = 1000`
- **Test steps:** 
  1. Mock `invoiceRepository.findByIdForUpdate` to return a valid invoice.
  2. Mock `userRepository.findById` to return a valid user.
  3. Call `paymentService.recordPayment(req)`.
- **Expected result:** Payment saved successfully, invoice updated to `PAID`.
- **Automation target:** `PaymentServiceTest.should_ReturnPaymentResponse_when_RecordPaymentValid()` (CSV row `TC-BILL-PAY-001`)
- **Coverage target:** Success path, status transition branch.

### TC-BILL-PAY-002: Successful Partial Payment
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Verify that submitting a payment less than the total invoice amount correctly marks the invoice as `PARTIALLY_PAID`.
- **Preconditions:** Valid active invoice with `UNPAID` status, authenticated staff user.
- **Input data:** `amount = 500`, `total_amount = 1000`
- **Test steps:** Execute `paymentService.recordPayment(req)`.
- **Expected result:** Payment saved successfully, invoice updated to `PARTIALLY_PAID`.
- **Automation target:** `PaymentServiceTest.should_ReturnPaymentResponse_when_RecordPaymentValid()` (CSV row `TC-BILL-PAY-002`)
- **Coverage target:** Success path, status transition branch.

### TC-BILL-PAY-003 & 004: Invalid Amount (Zero or Negative)
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject payments where the amount is `<= 0`.
- **Preconditions:** None
- **Input data:** `amount = 0` or `-100`
- **Test steps:** Execute `paymentService.recordPayment(req)`.
- **Expected result:** Exception thrown, payment is not saved.
- **Expected error code:** `ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_RecordPaymentFailsBusinessRules()` (CSV row `TC-BILL-PAY-003`, `TC-BILL-PAY-004`)
- **Coverage target:** Validation branch.

### TC-BILL-PAY-005: Invoice Not Found
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject payments targeting a non-existent invoice.
- **Preconditions:** None
- **Input data:** Invalid `invoiceId`
- **Test steps:** Mock repository to return `Optional.empty()`.
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_RecordPaymentFailsBusinessRules()` (CSV row `TC-BILL-PAY-005`)
- **Coverage target:** Validation branch.

### TC-BILL-PAY-006: Amount Exceeds Remaining Balance
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject payments where the amount is strictly greater than the remaining unpaid balance.
- **Preconditions:** Valid invoice.
- **Input data:** `amount = 1001`, `total_amount = 1000`
- **Test steps:** Execute `paymentService.recordPayment(req)`.
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_RecordPaymentFailsBusinessRules()` (CSV row `TC-BILL-PAY-006`)
- **Coverage target:** Validation branch.

### TC-BILL-PAY-007: Current Staff/User Not Found
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Ensure the system accurately retrieves the currently authenticated user recording the payment.
- **Preconditions:** Current user ID from Security Context is valid but user entity is missing from DB.
- **Input data:** `amount = 1000`
- **Test steps:** Mock `userRepository` to return empty.
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_ACC_002_USER_NOT_FOUND`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_RecordPaymentFailsBusinessRules()` (CSV row `TC-BILL-PAY-007`)
- **Coverage target:** Authorization branch.

---

## B. PaymentService.createOwnerPaymentRequest()

### TC-BILL-PAY-008: Owner Submits Valid Payment Request
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Allow an invoice owner to submit a payment successfully.
- **Preconditions:** Valid invoice, authenticated owner user.
- **Input data:** `amount = 1000`, `is_owner = true`
- **Test steps:** Execute `paymentService.createOwnerPaymentRequest(req)`.
- **Expected result:** Payment saved successfully, invoice updated to `PAID`.
- **Automation target:** `PaymentServiceTest.should_ReturnPaymentResponse_when_OwnerPaymentValid()` (CSV row `TC-BILL-PAY-008`)
- **Coverage target:** Success path.

### TC-BILL-PAY-009: Authenticated User Is Not Invoice Owner
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject payment request if the current user is not the owner of the invoice.
- **Preconditions:** Valid invoice, authenticated non-owner user.
- **Input data:** `is_owner = false`
- **Test steps:** Execute `paymentService.createOwnerPaymentRequest(req)`.
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_403_FORBIDDEN`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_OwnerPaymentFailsSecurityOrBusinessRules()` (CSV row `TC-BILL-PAY-009`)
- **Coverage target:** Authorization branch.

### TC-BILL-PAY-010, 011, 012: Invalid Amounts (Owner)
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject zero, negative, or excessive amounts from owner.
- **Preconditions:** Valid invoice, authenticated owner user.
- **Input data:** amount zero, negative, or exceeds remaining balance.
- **Test steps:** Execute `paymentService.createOwnerPaymentRequest(req)`.
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_BILLING_003_INVALID_PAYMENT_AMOUNT`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_OwnerPaymentFailsSecurityOrBusinessRules()` (CSV row `TC-BILL-PAY-010`, `TC-BILL-PAY-011`, `TC-BILL-PAY-012`)
- **Coverage target:** Validation branch.

### TC-BILL-PAY-013: Invoice Not Found (Owner)
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Handle invalid invoice ID gracefully.
- **Preconditions:** None
- **Input data:** Invalid `invoiceId`
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND`
- **Automation target:** `PaymentServiceTest.should_ThrowException_when_OwnerPaymentFailsSecurityOrBusinessRules()` (CSV row `TC-BILL-PAY-013`)
- **Coverage target:** Validation branch.

---

## C. PaymentService.updatePaymentStatus()

### TC-BILL-PAY-014: Payment Transitions to SUCCEEDED
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Changing a payment status to `SUCCEEDED` successfully applies the amount to the invoice.
- **Preconditions:** Valid payment existing in DB, `UNPAID` invoice.
- **Input data:** `statusCode = SUCCEEDED`
- **Test steps:** Execute `paymentService.updatePaymentStatus(...)`.
- **Expected result:** Invoice `paidAmountVnd` increases, invoice status transitions to `PAID`.
- **Automation target:** `PaymentServiceTest.should_UpdateInvoice_when_PaymentStatusTransitionsToSucceeded()` (CSV row `TC-BILL-PAY-014`)
- **Coverage target:** Status transition branch.

### TC-BILL-PAY-015: Payment Transitions to FAILED
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Changing a payment status to `FAILED` updates the payment but does NOT mark the invoice as paid.
- **Preconditions:** Valid payment existing in DB, `UNPAID` invoice.
- **Input data:** `statusCode = FAILED`
- **Test steps:** Execute `paymentService.updatePaymentStatus(...)`.
- **Expected result:** Payment `statusCode` updated to `FAILED`. Invoice remains `UNPAID`.
- **Automation target:** `PaymentServiceTest.should_UpdateInvoice_when_PaymentStatusTransitionsToSucceeded()` (CSV row `TC-BILL-PAY-015`)
- **Coverage target:** Status transition branch.

### TC-BILL-PAY-016: Payment Not Found
- **Feature / module:** Billing/Payment (`PaymentService`)
- **Test type:** Unit Test
- **Tool:** Mockito/JUnit
- **Priority:** P0
- **Objective:** Reject status update for invalid payment ID.
- **Preconditions:** None
- **Input data:** Invalid `paymentId`
- **Expected result:** Exception thrown.
- **Expected error code:** `ErrorCode.ERR_BILLING_002_INVOICE_NOT_FOUND`
- **Automation target:** `PaymentServiceTest.should_UpdateInvoice_when_PaymentStatusTransitionsToSucceeded()` (CSV row `TC-BILL-PAY-016`)
- **Coverage target:** Validation branch.

---

## D. InvoiceMapper

### TC-BILL-MAP-001: Maps Entity to Response Accurately
- **Feature / module:** Billing/Payment (`InvoiceMapper`)
- **Test type:** Unit Test
- **Tool:** Plain JUnit (no mocks required for mapstruct)
- **Priority:** P0
- **Objective:** Verify that `owner.id` becomes `ownerId` and monetary fields map exactly without precision loss.
- **Preconditions:** Complete `Invoice` entity object.
- **Input data:** `Invoice` with `totalAmountVnd = 1000`, `owner.id = UUID`
- **Test steps:** Call `mapper.toResponse(invoice)`.
- **Expected result:** `InvoiceResponse` has matching `ownerId`, `totalAmountVnd`, `statusCode`.
- **Automation target:** `InvoiceMapperTest.should_MapInvoiceToResponse_when_AllFieldsArePresent()`
- **Coverage target:** Mapper branch.

### TC-BILL-MAP-002: Maps Invoice Line Correctly
- **Feature / module:** Billing/Payment (`InvoiceMapper`)
- **Test type:** Unit Test
- **Tool:** Plain JUnit
- **Priority:** P0
- **Objective:** Verify that `InvoiceLine` properly maps its parent `invoice.id`, `serviceOrder.id`, and `medicine.id`.
- **Preconditions:** Valid `InvoiceLine` entity.
- **Input data:** `InvoiceLine` object.
- **Test steps:** Call `mapper.toLineResponse(line)`.
- **Expected result:** `InvoiceLineResponse` contains the exact nested IDs.
- **Automation target:** `InvoiceMapperTest.should_MapInvoiceToResponse_when_AllFieldsArePresent()`
- **Coverage target:** Mapper branch.

### TC-BILL-MAP-003: Null Input Handling
- **Feature / module:** Billing/Payment (`InvoiceMapper`)
- **Test type:** Unit Test
- **Tool:** Plain JUnit
- **Priority:** P0
- **Objective:** Ensure MapStruct handles null inputs gracefully.
- **Preconditions:** None.
- **Input data:** `null`
- **Test steps:** Call `mapper.toResponse(null)`.
- **Expected result:** Returns `null`.
- **Automation target:** `InvoiceMapperTest.should_ReturnNull_when_MappingNullEntity()`
- **Coverage target:** Mapper branch.
