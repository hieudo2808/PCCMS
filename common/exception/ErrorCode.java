package com.astral.express.pccms.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 1. System/HTTP Errors
    ERR_500_INTERNAL_SERVER("ERR_500_INTERNAL_SERVER", 500, "Internal server error"),
    ERR_400_BAD_REQUEST("ERR_400_BAD_REQUEST", 400, "Invalid request"),
    ERR_404_NOT_FOUND("ERR_404_NOT_FOUND", 404, "Resource not found"),
    ERR_VALIDATION_FAILED("ERR_VALIDATION_FAILED", 400, "Validation failed"),

    // 2. IAM (Auth/Role)
    ERR_401_UNAUTHORIZED("ERR_401_UNAUTHORIZED", 401, "Unauthorized"),
    ERR_403_FORBIDDEN("ERR_403_FORBIDDEN", 403, "Forbidden"),
    ERR_IAM_001_INVALID_CREDENTIALS("ERR_IAM_001_INVALID_CREDENTIALS", 400, "Invalid credentials"),
    ERR_IAM_002_ACCOUNT_LOCKED("ERR_IAM_002_ACCOUNT_LOCKED", 400, "Account is locked"),
    ERR_IAM_003_RATE_LIMITED("ERR_IAM_003_RATE_LIMITED", 429, "Rate limited"),

    // 3. Medical Care
    ERR_MED_001_RECORD_LOCKED("ERR_MED_001_RECORD_LOCKED", 400, "Medical record is locked"),
    ERR_MED_002_INSUFFICIENT_STOCK("ERR_MED_002_INSUFFICIENT_STOCK", 400, "Insufficient stock"),
    ERR_MED_003_INVALID_VITALS("ERR_MED_003_INVALID_VITALS", 400, "Invalid vitals"),
    ERR_MED_004_MEDICINE_NOT_FOUND("ERR_MED_004_MEDICINE_NOT_FOUND", 404, "Medicine not found"),
    ERR_MED_005_MEDICINE_CODE_EXISTS("ERR_MED_005_MEDICINE_CODE_EXISTS", 400, "Medicine code already exists"),
    ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND("ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND", 404, "Medicine category not found"),
    ERR_VACC_001_INVALID_DUE_DATE("ERR_VACC_001_INVALID_DUE_DATE", 400, "Invalid next due date"),

    // Medical Record
    ERR_MR_001_INVALID_TEMPERATURE("ERR_MR_001", 400, "Invalid temperature"),
    ERR_MR_002_INVALID_HEART_RATE("ERR_MR_002", 400, "Invalid heart rate"),
    ERR_MR_003_INVALID_RESPIRATORY_RATE("ERR_MR_003", 400, "Invalid respiratory rate"),
    ERR_MR_004_INVALID_SPO2("ERR_MR_004", 400, "Invalid SpO2"),
    ERR_MR_005_INVALID_WEIGHT("ERR_MR_005", 400, "Invalid weight"),
    ERR_MR_006_RECORD_NOT_DRAFT("ERR_MR_006", 400, "Medical record is not draft"),
    ERR_MR_007_MISSING_FINAL_DIAGNOSIS("ERR_MR_007", 400, "Missing final diagnosis"),
    ERR_MR_008_MISSING_VITAL_SIGNS("ERR_MR_008", 400, "Missing vital signs"),

    // 4. Account/Pet
    ERR_ACC_001_EMAIL_EXISTS("ERR_ACC_001_EMAIL_EXISTS", 400, "Email already exists"),
    ERR_ACC_002_USER_NOT_FOUND("ERR_ACC_002_USER_NOT_FOUND", 404, "User not found"),
    ERR_ACC_003_USER_ALREADY_LOCKED("ERR_ACC_003_USER_ALREADY_LOCKED", 400, "User already locked"),
    ERR_ACC_004_USER_ALREADY_DISABLED("ERR_ACC_004_USER_ALREADY_DISABLED", 400, "User already disabled"),
    ERR_ACC_005_DEFAULT_ROLE_NOT_FOUND("ERR_ACC_003_DEFAULT_ROLE_NOT_FOUND", 400, "Default role not found"),
    ERR_PET_001_NOT_FOUND("ERR_PET_001_NOT_FOUND", 404, "Pet not found"),
    ERR_PET_INVALID_AGE_DATA("ERR_PET_INVALID_AGE_DATA", 400, "Invalid pet age data"),
    ERR_PET_INVALID_WEIGHT("ERR_PET_INVALID_WEIGHT", 400, "Invalid pet weight"),

    // 5. Boarding, room, file, billing
    ERR_ROOM_001_ROOM_TYPE_NOT_FOUND("ERR_ROOM_001_ROOM_TYPE_NOT_FOUND", 404, "Room type not found"),
    ERR_ROOM_002_ROOM_NOT_FOUND("ERR_ROOM_002_ROOM_NOT_FOUND", 404, "Room not found"),
    ERR_ROOM_003_ROOM_UNAVAILABLE("ERR_ROOM_003_ROOM_UNAVAILABLE", 409, "Room is unavailable for this time range"),
    ERR_BOARDING_001_BOOKING_NOT_FOUND("ERR_BOARDING_001_BOOKING_NOT_FOUND", 404, "Boarding booking not found"),
    ERR_BOARDING_002_INVALID_TIME_RANGE("ERR_BOARDING_002_INVALID_TIME_RANGE", 400, "Invalid boarding time range"),
    ERR_BOARDING_003_INVALID_STATUS_TRANSITION("ERR_BOARDING_003_INVALID_STATUS_TRANSITION", 400, "Invalid boarding status transition"),
    ERR_BOARDING_004_CARE_LOG_DUPLICATED("ERR_BOARDING_004_CARE_LOG_DUPLICATED", 409, "Care log already exists for this period"),
    ERR_BOARDING_005_SESSION_NOT_FOUND("ERR_BOARDING_005_SESSION_NOT_FOUND", 404, "Boarding session not found"),
    ERR_BOARDING_006_DUPLICATE_BOOKING("ERR_BOARDING_006_DUPLICATE_BOOKING", 409, "Duplicate boarding booking exists for this time range"),
    ERR_FILE_001_INVALID_IMAGE("ERR_FILE_001_INVALID_IMAGE", 400, "Invalid image file"),
    ERR_BILLING_001_INVOICE_ALREADY_EXISTS("ERR_BILLING_001_INVOICE_ALREADY_EXISTS", 409, "Invoice already exists for this service"),

    // 6. Grooming
    ERR_GROOMING_001_TICKET_NOT_FOUND("ERR_GROOMING_001_TICKET_NOT_FOUND", 404, "Grooming ticket not found"),
    ERR_GROOMING_002_SERVICE_NOT_FOUND("ERR_GROOMING_002_SERVICE_NOT_FOUND", 404, "Grooming service not found"),
    ERR_GROOMING_003_INVALID_TIME_RANGE("ERR_GROOMING_003_INVALID_TIME_RANGE", 400, "Invalid grooming time range"),
    ERR_GROOMING_004_INVALID_STATUS_TRANSITION("ERR_GROOMING_004_INVALID_STATUS_TRANSITION", 400, "Invalid grooming status transition"),
    ERR_GROOMING_005_STATION_NOT_FOUND("ERR_GROOMING_005_STATION_NOT_FOUND", 404, "Grooming station not found"),
    ERR_GROOMING_006_STATION_UNAVAILABLE("ERR_GROOMING_006_STATION_UNAVAILABLE", 409, "Grooming station is unavailable for this time range"),
    ERR_GROOMING_007_SERVICE_CODE_EXISTS("ERR_GROOMING_007_SERVICE_CODE_EXISTS", 409, "Grooming service code already exists"),
    ERR_GROOMING_008_STATION_CODE_EXISTS("ERR_GROOMING_008_STATION_CODE_EXISTS", 409, "Grooming station code already exists"),
    ERR_GROOMING_009_DUPLICATE_BOOKING("ERR_GROOMING_009_DUPLICATE_BOOKING", 409, "Duplicate grooming booking exists for this time range");

    private final String errorCode;
    private final int httpStatus;
    private final String message;

    ErrorCode(String errorCode, int httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
