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
    ERR_OTP_001_INVALID_OR_EXPIRED("ERR_OTP_001_INVALID_OR_EXPIRED", 400, "Invalid or expired OTP"),
    ERR_OTP_002_TOO_MANY_ATTEMPTS("ERR_OTP_002_TOO_MANY_ATTEMPTS", 400, "Too many OTP attempts"),

    // 3. Medical Care
    ERR_MED_001_RECORD_LOCKED("ERR_MED_001_RECORD_LOCKED", 400, "Medical record is locked"),
    ERR_MED_002_INSUFFICIENT_STOCK("ERR_MED_002_INSUFFICIENT_STOCK", 400, "Insufficient stock"),
    ERR_MED_003_INVALID_VITALS("ERR_MED_003_INVALID_VITALS", 400, "Invalid vitals"),
    ERR_MED_004_MEDICINE_NOT_FOUND("ERR_MED_004_MEDICINE_NOT_FOUND", 404, "Medicine not found"),
    ERR_MED_005_MEDICINE_CODE_EXISTS("ERR_MED_005_MEDICINE_CODE_EXISTS", 400, "Medicine code already exists"),
    ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND("ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND", 404, "Medicine category not found"),
    ERR_MED_007_MEDICINE_NAME_EXISTS("ERR_MED_007_MEDICINE_NAME_EXISTS", 400, "Medicine name already exists"),
    ERR_MED_008_MEDICINE_IN_USE("ERR_MED_008_MEDICINE_IN_USE", 400, "Medicine is in use"),
    ERR_MED_009_CATEGORY_NAME_EXISTS("ERR_MED_009_CATEGORY_NAME_EXISTS", 409, "Medicine category name already exists"),
    ERR_MED_010_CATEGORY_IN_USE("ERR_MED_010_CATEGORY_IN_USE", 409, "Medicine category is in use"),
    ERR_MED_011_TEMPLATE_NOT_FOUND("ERR_MED_011_TEMPLATE_NOT_FOUND", 404, "Medicine usage template not found"),
    ERR_MED_012_TEMPLATE_NAME_EXISTS("ERR_MED_012_TEMPLATE_NAME_EXISTS", 400, "Medicine usage template name already exists for this medicine"),
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
    ERR_MR_009_LAB_RESULT_NOT_FOUND("ERR_MR_009", 404, "Lab result not found"),

    // 4. Account/Pet
    ERR_ACC_001_EMAIL_EXISTS("ERR_ACC_001_EMAIL_EXISTS", 400, "Email already exists"),
    ERR_ACC_002_USER_NOT_FOUND("ERR_ACC_002_USER_NOT_FOUND", 404, "User not found"),
    ERR_ACC_003_USER_ALREADY_LOCKED("ERR_ACC_003_USER_ALREADY_LOCKED", 400, "User already locked"),
    ERR_ACC_004_USER_ALREADY_DISABLED("ERR_ACC_004_USER_ALREADY_DISABLED", 400, "User already disabled"),
    ERR_ACC_005_DEFAULT_ROLE_NOT_FOUND("ERR_ACC_003_DEFAULT_ROLE_NOT_FOUND", 400, "Default role not found"),
    ERR_ACC_006_ROLE_NOT_FOUND("ERR_ACC_006_ROLE_NOT_FOUND", 404, "Role not found"),
    ERR_ACC_007_SEARCH_CRITERIA_REQUIRED("ERR_ACC_007_SEARCH_CRITERIA_REQUIRED", 400, "Search criteria required"),
    ERR_ACC_008_PHONE_EXISTS("ERR_ACC_008_PHONE_EXISTS", 400, "Phone already exists"),
    ERR_ACC_009_PROTECTED_ADMIN_ACCOUNT("ERR_ACC_009_PROTECTED_ADMIN_ACCOUNT", 403, "Admin accounts cannot be managed here"),
    ERR_PET_001_NOT_FOUND("ERR_PET_001_NOT_FOUND", 404, "Pet not found"),
    ERR_PET_INVALID_AGE_DATA("ERR_PET_INVALID_AGE_DATA", 400, "Invalid pet age data"),
    ERR_PET_INVALID_WEIGHT("ERR_PET_INVALID_WEIGHT", 400, "Invalid pet weight"),
    ERR_PET_SPECIES_NOT_FOUND("ERR_PET_SPECIES_NOT_FOUND", 404, "Pet species not found"),
    ERR_PET_BREED_NOT_FOUND("ERR_PET_BREED_NOT_FOUND", 404, "Pet breed not found"),
    ERR_PET_BREED_SPECIES_MISMATCH("ERR_PET_BREED_SPECIES_MISMATCH", 400, "Breed does not match species"),

    // Reception
    ERR_REC_001_APPOINTMENT_NOT_FOUND("ERR_REC_001_APPOINTMENT_NOT_FOUND", 404, "Appointment not found"),
    ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE("ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE", 400, "Appointment not receivable"),
    ERR_REC_003_PHONE_REQUIRED("ERR_REC_003_PHONE_REQUIRED", 400, "Phone number required"),
    ERR_REC_004_ACTIVE_BOARDING_SESSION_NOT_FOUND("ERR_REC_004_ACTIVE_BOARDING_SESSION_NOT_FOUND", 404, "Active boarding session not found"),
    ERR_REC_005_INVALID_CARE_LOG("ERR_REC_005_INVALID_CARE_LOG", 400, "Invalid care log"),
    ERR_REC_006_GROOMING_TICKET_NOT_FOUND("ERR_REC_006_GROOMING_TICKET_NOT_FOUND", 404, "Grooming ticket not found"),
    ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION("ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION", 400, "Invalid grooming status transition"),

    // Appointments
    ERR_APT_001_NOT_FOUND("ERR_APT_001_NOT_FOUND", 404, "Appointment not found"),
    ERR_APT_002_PAST_DATETIME("ERR_APT_002_PAST_DATETIME", 400, "Cannot book appointment in the past"),
    ERR_APT_003_ALREADY_CANCELLED("ERR_APT_003_ALREADY_CANCELLED", 400, "Appointment is already cancelled"),
    ERR_APT_004_ALREADY_CHECKED_IN("ERR_APT_004_ALREADY_CHECKED_IN", 400, "Appointment is already checked in"),
    ERR_APT_005_NO_VET_AVAILABLE("ERR_APT_005_NO_VET_AVAILABLE", 400, "No veterinarian available"),
    ERR_APT_006_SERVICE_NOT_FOUND("ERR_APT_006_SERVICE_NOT_FOUND", 404, "Service not found"),
    ERR_APT_007_CANNOT_CANCEL("ERR_APT_007_CANNOT_CANCEL", 400, "Cannot cancel appointment"),
    ERR_APT_008_PHONE_REQUIRED("ERR_APT_008_PHONE_REQUIRED", 400, "Phone number is required"),
    ERR_APT_009_SLOT_FULL("ERR_APT_009_SLOT_FULL", 400, "Appointment slot is full"),
    ERR_APT_010_OUT_OF_BUSINESS_HOURS("ERR_APT_010_OUT_OF_BUSINESS_HOURS", 400, "Cannot book appointment outside of business hours"),
    ERR_APT_011_INVALID_TYPE("ERR_APT_011_INVALID_TYPE", 400, "Invalid appointment type for this operation"),

    // 5. Boarding, room, file, billing
    ERR_ROOM_001_ROOM_TYPE_NOT_FOUND("ERR_ROOM_001_ROOM_TYPE_NOT_FOUND", 404, "Room type not found"),
    ERR_ROOM_002_ROOM_NOT_FOUND("ERR_ROOM_002_ROOM_NOT_FOUND", 404, "Room not found"),
    ERR_ROOM_003_ROOM_UNAVAILABLE("ERR_ROOM_003_ROOM_UNAVAILABLE", 409, "Room is unavailable for this time range"),
    ERR_ROOM_004_TYPE_NOT_FOUND("ERR_ROOM_004_TYPE_NOT_FOUND", 404, "Room type not found"),
    ERR_ROOM_005_TYPE_CODE_EXISTS("ERR_ROOM_005_TYPE_CODE_EXISTS", 409, "Room type code already exists"),
    ERR_ROOM_007_TYPE_IN_USE("ERR_ROOM_007_TYPE_IN_USE", 400, "Room type is in use"),
    ERR_ROOM_001_NOT_FOUND("ERR_ROOM_001_NOT_FOUND", 404, "Room not found"),
    ERR_ROOM_006_ROOM_OCCUPIED("ERR_ROOM_006_ROOM_OCCUPIED", 409, "Room is occupied"),
    ERR_ROOM_002_CODE_EXISTS("ERR_ROOM_002_CODE_EXISTS", 409, "Room code exists"),
    ERR_ROOM_003_NAME_EXISTS("ERR_ROOM_003_NAME_EXISTS", 409, "Room name exists"),

    ERR_BOARDING_001_BOOKING_NOT_FOUND("ERR_BOARDING_001_BOOKING_NOT_FOUND", 404, "Boarding booking not found"),
    ERR_BOARDING_002_INVALID_TIME_RANGE("ERR_BOARDING_002_INVALID_TIME_RANGE", 400, "Invalid boarding time range"),
    ERR_BOARDING_003_INVALID_STATUS_TRANSITION("ERR_BOARDING_003_INVALID_STATUS_TRANSITION", 400, "Invalid boarding status transition"),
    ERR_BRG_001_SESSION_NOT_FOUND("ERR_BRG_001_SESSION_NOT_FOUND", 404, "Boarding session not found"),
    ERR_BRG_002_SESSION_NOT_ACTIVE("ERR_BRG_002_SESSION_NOT_ACTIVE", 400, "Boarding session not active"),

    ERR_BOARDING_004_CARE_LOG_DUPLICATED("ERR_BOARDING_004_CARE_LOG_DUPLICATED", 409, "Care log already exists for this period"),
    ERR_BOARDING_005_SESSION_NOT_FOUND("ERR_BOARDING_005_SESSION_NOT_FOUND", 404, "Boarding session not found"),
    ERR_BOARDING_006_DUPLICATE_BOOKING("ERR_BOARDING_006_DUPLICATE_BOOKING", 409, "Duplicate boarding booking exists for this time range"),
    ERR_BOARDING_007_CARE_LOG_LOCKED("ERR_BOARDING_007_CARE_LOG_LOCKED", 403, "Care log is locked"),
    ERR_FILE_001_INVALID_IMAGE("ERR_FILE_001_INVALID_IMAGE", 400, "Invalid image file"),
    ERR_BILLING_001_INVOICE_ALREADY_EXISTS("ERR_BILLING_001_INVOICE_ALREADY_EXISTS", 409, "Invoice already exists for this service"),
    ERR_BILLING_002_INVOICE_NOT_FOUND("ERR_BILLING_002_INVOICE_NOT_FOUND", 404, "Invoice not found"),
    ERR_BILLING_003_INVALID_PAYMENT_AMOUNT("ERR_BILLING_003_INVALID_PAYMENT_AMOUNT", 400, "Invalid payment amount"),
    ERR_NOTIFICATION_001_NOT_FOUND("ERR_NOTIFICATION_001_NOT_FOUND", 404, "Notification not found"),

    // 6. Grooming
    ERR_GROOMING_001_TICKET_NOT_FOUND("ERR_GROOMING_001_TICKET_NOT_FOUND", 404, "Grooming ticket not found"),
    ERR_GROOMING_002_SERVICE_NOT_FOUND("ERR_GROOMING_002_SERVICE_NOT_FOUND", 404, "Grooming service not found"),
    ERR_GROOMING_003_INVALID_TIME_RANGE("ERR_GROOMING_003_INVALID_TIME_RANGE", 400, "Invalid grooming time range"),
    ERR_GROOMING_004_INVALID_STATUS_TRANSITION("ERR_GROOMING_004_INVALID_STATUS_TRANSITION", 400, "Invalid grooming status transition"),
    ERR_GROOMING_005_STATION_NOT_FOUND("ERR_GROOMING_005_STATION_NOT_FOUND", 404, "Grooming station not found"),
    ERR_GROOMING_006_STATION_UNAVAILABLE("ERR_GROOMING_006_STATION_UNAVAILABLE", 409, "Grooming station is unavailable for this time range"),
    ERR_GROOMING_007_SERVICE_CODE_EXISTS("ERR_GROOMING_007_SERVICE_CODE_EXISTS", 409, "Grooming service code already exists"),
    ERR_GROOMING_008_STATION_CODE_EXISTS("ERR_GROOMING_008_STATION_CODE_EXISTS", 409, "Grooming station code already exists"),
    ERR_GROOMING_009_DUPLICATE_BOOKING("ERR_GROOMING_009_DUPLICATE_BOOKING", 409, "Duplicate grooming booking exists for this time range"),

    // 7. Service Catalog
    ERR_SVC_001_NOT_FOUND("ERR_SVC_001_NOT_FOUND", 404, "Service not found"),
    ERR_SVC_002_CODE_EXISTS("ERR_SVC_002_CODE_EXISTS", 409, "Service code already exists"),
    ERR_SVC_003_NAME_EXISTS("ERR_SVC_003_NAME_EXISTS", 409, "Service name already exists"),
    ERR_SVC_004_IN_USE("ERR_SVC_004_IN_USE", 400, "Service is in use");

    private final String errorCode;
    private final int httpStatus;
    private final String message;

    ErrorCode(String errorCode, int httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
