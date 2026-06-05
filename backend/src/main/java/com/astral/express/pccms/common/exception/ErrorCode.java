package com.astral.express.pccms.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 1. System/HTTP Errors
    ERR_500_INTERNAL_SERVER("ERR_500_INTERNAL_SERVER", 500, "Lỗi máy chủ cục bộ"),
    ERR_400_BAD_REQUEST("ERR_400_BAD_REQUEST", 400, "Request không hợp lệ"),
    ERR_404_NOT_FOUND("ERR_404_NOT_FOUND", 404, "Không tìm thấy dữ liệu"),
    ERR_VALIDATION_FAILED("ERR_VALIDATION_FAILED", 400, "Dữ liệu đầu vào không hợp lệ"),

    // 2. IAM (Auth/Role)
    ERR_401_UNAUTHORIZED("ERR_401_UNAUTHORIZED", 401, "Chưa đăng nhập hoặc token hết hạn"),
    ERR_403_FORBIDDEN("ERR_403_FORBIDDEN", 403, "Không có quyền truy cập tài nguyên này"),
    ERR_IAM_001_INVALID_CREDENTIALS("ERR_IAM_001_INVALID_CREDENTIALS", 400, "Thông tin đăng nhập không chính xác"),
    ERR_IAM_002_ACCOUNT_LOCKED("ERR_IAM_002_ACCOUNT_LOCKED", 400, "Tài khoản đã bị khóa"),
    ERR_IAM_003_RATE_LIMITED("ERR_IAM_003_RATE_LIMITED", 429, "Rate limited"),

    // 3. Medical Care
    ERR_MED_001_RECORD_LOCKED("ERR_MED_001_RECORD_LOCKED", 400, "Bệnh án đã chốt, không thể chỉnh sửa"),
    ERR_MED_002_INSUFFICIENT_STOCK("ERR_MED_002_INSUFFICIENT_STOCK", 400, "Không đủ thuốc trong kho"),
    ERR_MED_003_INVALID_VITALS("ERR_MED_003_INVALID_VITALS", 400, "Chỉ số sinh hiệu không hợp lệ"),
    ERR_MED_004_MEDICINE_NOT_FOUND("ERR_MED_004_MEDICINE_NOT_FOUND", 404, "Không tìm thấy thuốc"),
    ERR_MED_005_MEDICINE_CODE_EXISTS("ERR_MED_005_MEDICINE_CODE_EXISTS", 400, "Mã thuốc đã tồn tại"),
    ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND("ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND", 404, "Không tìm thấy danh mục thuốc"),
    ERR_VACC_001_INVALID_DUE_DATE("ERR_VACC_001_INVALID_DUE_DATE", 400, "Ngày hẹn tiêm tiếp theo không được trước ngày tiêm"),
    
    // Medical Record
    ERR_MR_001_INVALID_TEMPERATURE("ERR_MR_001", 400, "Nhiệt độ không hợp lệ"),
    ERR_MR_002_INVALID_HEART_RATE("ERR_MR_002", 400, "Nhịp tim không hợp lệ"),
    ERR_MR_003_INVALID_RESPIRATORY_RATE("ERR_MR_003", 400, "Nhịp thở không hợp lệ"),
    ERR_MR_004_INVALID_SPO2("ERR_MR_004", 400, "SpO2 không hợp lệ"),
    ERR_MR_005_INVALID_WEIGHT("ERR_MR_005", 400, "Cân nặng không hợp lệ"),
    ERR_MR_006_RECORD_NOT_DRAFT("ERR_MR_006", 400, "Không thể cập nhật bệnh án đã chốt hoặc hủy"),
    ERR_MR_007_MISSING_FINAL_DIAGNOSIS("ERR_MR_007", 400, "Phải có chẩn đoán cuối cùng để chốt bệnh án"),
    ERR_MR_008_MISSING_VITAL_SIGNS("ERR_MR_008", 400, "Phải có ít nhất một sinh hiệu để chốt bệnh án"),

    // 4. Account/Pet
    ERR_ACC_001_EMAIL_EXISTS("ERR_ACC_001_EMAIL_EXISTS", 400, "Email đã được đăng ký"),
    ERR_ACC_002_USER_NOT_FOUND("ERR_ACC_002_USER_NOT_FOUND", 404, "Không tìm thấy thông tin người dùng"),
    ERR_ACC_003_USER_ALREADY_LOCKED("ERR_ACC_003_USER_ALREADY_LOCKED", 400, "Tài khoản người dùng đã bị khóa"),
    ERR_ACC_004_USER_ALREADY_DISABLED("ERR_ACC_004_USER_ALREADY_DISABLED", 400, "Tài khoản người dùng đã bị vô hiệu hóa"),
    ERR_ACC_005_DEFAULT_ROLE_NOT_FOUND("ERR_ACC_003_DEFAULT_ROLE_NOT_FOUND", 400, "Không tìm thấy role mặc định"),
    ERR_PET_001_NOT_FOUND("ERR_PET_001_NOT_FOUND", 404, "Không tìm thấy thú cưng"),
    ERR_PET_INVALID_AGE_DATA("ERR_PET_INVALID_AGE_DATA", 400, "Phải nhập ngày sinh hoặc tháng tuổi dự kiến hợp lệ"),
    ERR_PET_INVALID_WEIGHT("ERR_PET_INVALID_WEIGHT", 400, "Cân nặng phải là một con số hợp lệ"),
    ERR_PET_SPECIES_NOT_FOUND("ERR_PET_SPECIES_NOT_FOUND", 404, "Không tìm thấy loài thú cưng"),
    ERR_PET_BREED_NOT_FOUND("ERR_PET_BREED_NOT_FOUND", 404, "Không tìm thấy giống thú cưng"),
    ERR_PET_BREED_SPECIES_MISMATCH("ERR_PET_BREED_SPECIES_MISMATCH", 400, "Giống thú cưng không thuộc loài đã chọn");

    private final String errorCode;
    private final int httpStatus;
    private final String message;

    ErrorCode(String errorCode, int httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
