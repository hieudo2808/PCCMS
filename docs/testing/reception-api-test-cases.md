# Reception API Test Cases

## Overview
This document specifies the API test cases for the Reception module controllers:
- `AppointmentReceptionController` (`/v1/reception/appointments`)
- `BoardingCareLogController` (`/v1/reception/boarding`)
- `GroomingBoardController` (`/v1/reception/grooming-tickets`)

**Testing Convention:**
- Use `MockMvc` with `.contextPath("/api")` assuming `ApiConfig` prepends `/api`.
- Mock underlying services (e.g. `AppointmentReceptionService`).
- Expect `ApiResponse` wrapper in JSON responses (e.g., `$.code`, `$.message`, `$.data`).

---

## 1. Appointment Reception API

### API-REC-001: List appointments successfully
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments`
* **HTTP Method:** `GET`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** None
* **Request params:** None
* **Mocked service behavior:** `appointmentReceptionService.listAppointments("", null)` returns list of `AppointmentReceptionResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Lấy danh sách lịch hẹn thành công"`, `$.data` is an array.
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_return_list_of_appointments()`
* **Priority:** P1

### API-REC-002: Quick check-in successfully (Create & Receive)
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/quick`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** None
* **Request body:** valid `QuickAppointmentRequest` (`phone`, `ownerName`, `petName`, `symptomText`)
* **Mocked service behavior:** `appointmentReceptionService.quickCreateAndReceive(any())` returns valid `AppointmentReceptionResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Tạo nhanh và tiếp nhận thành công"`, `$.data` is not null.
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_create_and_receive_quick_appointment_successfully()`
* **Priority:** P0

### API-REC-003: Quick check-in fails on validation
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/quick`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Invalid input data
* **Request body:** `QuickAppointmentRequest` with missing `phone` and `ownerName`.
* **Mocked service behavior:** Not called.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.message` = `"Validation failed"` (or equivalent), `$.errors` contains fields.
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_return_400_when_quick_appointment_validation_fails()`
* **Priority:** P0

### API-REC-004: Receive appointment successfully
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/{id}/receive`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Appointment is in a valid state to receive.
* **Request path:** `id` = valid UUID
* **Request body:** `AppointmentReceiveRequest` (optional fields)
* **Mocked service behavior:** `appointmentReceptionService.receive(id, request)` returns `AppointmentReceptionResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Tiếp nhận thành công"`, `$.data` is not null.
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_receive_appointment_successfully()`
* **Priority:** P0

### API-REC-005: Receive appointment fails - Not found
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/{id}/receive`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Appointment does not exist.
* **Request path:** `id` = non-existent UUID
* **Request body:** `AppointmentReceiveRequest`
* **Mocked service behavior:** `appointmentReceptionService.receive(id, request)` throws `BusinessException` with `ERR_REC_001_APPOINTMENT_NOT_FOUND`.
* **Expected HTTP status:** `404 Not Found`
* **Expected JSON fields:** `$.code` = `404`, `$.errorCode` = `"ERR_REC_001_APPOINTMENT_NOT_FOUND"`
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_return_404_when_receiving_non_existent_appointment()`
* **Priority:** P0

### API-REC-006: Receive appointment fails - Not receivable
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/{id}/receive`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Appointment is cancelled or already checked in.
* **Request path:** `id` = valid UUID
* **Request body:** `AppointmentReceiveRequest`
* **Mocked service behavior:** `appointmentReceptionService.receive(id, request)` throws `BusinessException` with `ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE`.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.errorCode` = `"ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE"`
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_return_400_when_appointment_not_receivable()`
* **Priority:** P0

### API-REC-007: Cancel appointment successfully
* **Module:** Reception
* **Controller:** `AppointmentReceptionController`
* **Endpoint:** `/api/v1/reception/appointments/{id}/cancel`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Appointment exists.
* **Request path:** `id` = valid UUID
* **Request body:** `AppointmentCancelRequest`
* **Mocked service behavior:** `appointmentReceptionService.cancel(id, request)` returns `AppointmentReceptionResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Hủy lịch hẹn thành công"`
* **Automation target:** `AppointmentReceptionControllerTest` -> `should_cancel_appointment_successfully()`
* **Priority:** P1

---

## 2. Boarding Care Log API

### API-REC-008: List bookings successfully
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/bookings`
* **HTTP Method:** `GET`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** None
* **Request params:** None
* **Mocked service behavior:** `boardingCareLogService.listBookings("", null)` returns list of `BoardingBookingResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Lấy danh sách lưu trú thành công"`
* **Automation target:** `BoardingCareLogControllerTest` -> `should_return_list_of_bookings()`
* **Priority:** P1

### API-REC-009: List care logs successfully
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/care-logs`
* **HTTP Method:** `GET`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** None
* **Request params:** `sessionId` (optional), `petId` (optional)
* **Mocked service behavior:** `boardingCareLogService.listCareLogs(sessionId, petId)` returns list of `CareLogResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Lấy nhật ký lưu trú thành công"`
* **Automation target:** `BoardingCareLogControllerTest` -> `should_return_list_of_care_logs()`
* **Priority:** P1

### API-REC-010: Save care log successfully
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/care-logs`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Valid active boarding session exists.
* **Request body:** valid `CareLogRequest` (`periodCode`, `feedingStatus`, `hygieneStatus`)
* **Mocked service behavior:** `boardingCareLogService.saveCareLog(any())` returns `CareLogResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Lưu nhật ký lưu trú thành công"`
* **Automation target:** `BoardingCareLogControllerTest` -> `should_save_care_log_successfully()`
* **Priority:** P0

### API-REC-011: Save care log fails on validation
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/care-logs`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Invalid input
* **Request body:** `CareLogRequest` missing `periodCode` or `feedingStatus`.
* **Mocked service behavior:** Not called.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.errors` exists.
* **Automation target:** `BoardingCareLogControllerTest` -> `should_return_400_when_care_log_validation_fails()`
* **Priority:** P0

### API-REC-012: Save care log fails - Missing session and pet
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/care-logs`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Request has no `sessionId` and no `petId`.
* **Request body:** Valid formatting but missing session/pet association.
* **Mocked service behavior:** `boardingCareLogService.saveCareLog(any())` throws `BusinessException` with `ERR_REC_005_INVALID_CARE_LOG`.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.errorCode` = `"ERR_REC_005_INVALID_CARE_LOG"`
* **Automation target:** `BoardingCareLogControllerTest` -> `should_return_400_when_care_log_is_invalid()`
* **Priority:** P0

### API-REC-013: Upload care log media successfully
* **Module:** Reception
* **Controller:** `BoardingCareLogController`
* **Endpoint:** `/api/v1/reception/boarding/care-logs/{id}/media`
* **HTTP Method:** `POST`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Care log exists.
* **Request path:** `id` = valid UUID
* **Request part:** `MultipartFile file`
* **Mocked service behavior:** `boardingCareLogService.uploadMedia(id, file)` returns `CareLogMediaResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Tải ảnh/video thành công"`
* **Automation target:** `BoardingCareLogControllerTest` -> `should_upload_care_log_media_successfully()`
* **Priority:** P1

---

## 3. Grooming Board API

### API-REC-014: List grooming tickets successfully
* **Module:** Reception
* **Controller:** `GroomingBoardController`
* **Endpoint:** `/api/v1/reception/grooming-tickets`
* **HTTP Method:** `GET`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** None
* **Request params:** None
* **Mocked service behavior:** `groomingBoardService.listTickets("", null)` returns list of `GroomingTicketResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Lấy bảng dịch vụ làm đẹp thành công"`
* **Automation target:** `GroomingBoardControllerTest` -> `should_return_list_of_grooming_tickets()`
* **Priority:** P1

### API-REC-015: Update grooming status successfully
* **Module:** Reception
* **Controller:** `GroomingBoardController`
* **Endpoint:** `/api/v1/reception/grooming-tickets/{id}/status`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Valid state transition.
* **Request path:** `id` = valid UUID
* **Request body:** `GroomingStatusUpdateRequest` with valid `statusCode`.
* **Mocked service behavior:** `groomingBoardService.updateStatus(id, request)` returns `GroomingTicketResponse`.
* **Expected HTTP status:** `200 OK`
* **Expected JSON fields:** `$.code` = `200`, `$.message` = `"Cập nhật trạng thái làm đẹp thành công"`
* **Automation target:** `GroomingBoardControllerTest` -> `should_update_grooming_status_successfully()`
* **Priority:** P0

### API-REC-016: Update grooming status fails on validation
* **Module:** Reception
* **Controller:** `GroomingBoardController`
* **Endpoint:** `/api/v1/reception/grooming-tickets/{id}/status`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Invalid input.
* **Request path:** `id` = valid UUID
* **Request body:** `GroomingStatusUpdateRequest` with missing or invalid `statusCode`.
* **Mocked service behavior:** Not called.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.errors` exists.
* **Automation target:** `GroomingBoardControllerTest` -> `should_return_400_when_grooming_status_validation_fails()`
* **Priority:** P0

### API-REC-017: Update grooming status fails - Not found
* **Module:** Reception
* **Controller:** `GroomingBoardController`
* **Endpoint:** `/api/v1/reception/grooming-tickets/{id}/status`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Grooming ticket does not exist.
* **Request path:** `id` = non-existent UUID
* **Request body:** Valid `GroomingStatusUpdateRequest`
* **Mocked service behavior:** `groomingBoardService.updateStatus(id, request)` throws `BusinessException` with `ERR_REC_006_GROOMING_TICKET_NOT_FOUND`.
* **Expected HTTP status:** `404 Not Found`
* **Expected JSON fields:** `$.code` = `404`, `$.errorCode` = `"ERR_REC_006_GROOMING_TICKET_NOT_FOUND"`
* **Automation target:** `GroomingBoardControllerTest` -> `should_return_404_when_updating_status_of_non_existent_grooming_ticket()`
* **Priority:** P0

### API-REC-018: Update grooming status fails - Invalid transition
* **Module:** Reception
* **Controller:** `GroomingBoardController`
* **Endpoint:** `/api/v1/reception/grooming-tickets/{id}/status`
* **HTTP Method:** `PATCH`
* **Test type:** API Test
* **Tool:** `MockMvc`
* **Preconditions:** Attempt to transition backwards (e.g. `COMPLETED` -> `IN_SERVICE`).
* **Request path:** `id` = valid UUID
* **Request body:** Valid `GroomingStatusUpdateRequest`
* **Mocked service behavior:** `groomingBoardService.updateStatus(id, request)` throws `BusinessException` with `ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION`.
* **Expected HTTP status:** `400 Bad Request`
* **Expected JSON fields:** `$.code` = `400`, `$.errorCode` = `"ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION"`
* **Automation target:** `GroomingBoardControllerTest` -> `should_return_400_when_grooming_status_transition_is_invalid()`
* **Priority:** P0
