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
    ERR_MED_007_MEDICINE_NAME_EXISTS("ERR_MED_007_MEDICINE_NAME_EXISTS", 400, "Tên thuốc và đơn vị đã tồn tại"),
    ERR_MED_008_MEDICINE_IN_USE("ERR_MED_008_MEDICINE_IN_USE", 400, "Thuốc đang được sử dụng, không thể xóa"),
    ERR_MED_009_CATEGORY_NAME_EXISTS("ERR_MED_009_CATEGORY_NAME_EXISTS", 400, "Tên nhóm thuốc đã tồn tại"),
    ERR_MED_010_CATEGORY_IN_USE("ERR_MED_010_CATEGORY_IN_USE", 400, "Nhóm thuốc đang được sử dụng, không thể xóa"),

    // Catalog — Service
    ERR_SVC_001_NOT_FOUND("ERR_SVC_001_NOT_FOUND", 404, "Không tìm thấy dịch vụ"),
    ERR_SVC_002_CODE_EXISTS("ERR_SVC_002_CODE_EXISTS", 400, "Mã dịch vụ đã tồn tại"),
    ERR_SVC_003_NAME_EXISTS("ERR_SVC_003_NAME_EXISTS", 400, "Tên dịch vụ đã tồn tại"),
    ERR_SVC_004_IN_USE("ERR_SVC_004_IN_USE", 400, "Dịch vụ đang được sử dụng, không thể xóa"),

    // Catalog — Room
    ERR_ROOM_001_NOT_FOUND("ERR_ROOM_001_NOT_FOUND", 404, "Không tìm thấy phòng lưu trú"),
    ERR_ROOM_002_CODE_EXISTS("ERR_ROOM_002_CODE_EXISTS", 400, "Mã phòng đã tồn tại"),
    ERR_ROOM_003_NAME_EXISTS("ERR_ROOM_003_NAME_EXISTS", 400, "Tên phòng đã tồn tại"),
    ERR_ROOM_004_TYPE_NOT_FOUND("ERR_ROOM_004_TYPE_NOT_FOUND", 404, "Không tìm thấy loại phòng"),
    ERR_ROOM_005_TYPE_CODE_EXISTS("ERR_ROOM_005_TYPE_CODE_EXISTS", 400, "Mã loại phòng đã tồn tại"),
    ERR_ROOM_006_ROOM_OCCUPIED("ERR_ROOM_006_ROOM_OCCUPIED", 400, "Phòng đang sử dụng, không thể xóa"),
    ERR_ROOM_007_TYPE_IN_USE("ERR_ROOM_007_TYPE_IN_USE", 400, "Loại phòng đang được sử dụng, không thể xóa"),
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
    ERR_PET_BREED_SPECIES_MISMATCH("ERR_PET_BREED_SPECIES_MISMATCH", 400, "Giống thú cưng không thuộc loài đã chọn"),

    // 5. Appointment & Reception
    ERR_APT_001_NOT_FOUND("ERR_APT_001_NOT_FOUND", 404, "Không tìm thấy lịch hẹn"),
    ERR_APT_002_PAST_DATETIME("ERR_APT_002_PAST_DATETIME", 400, "Thời gian hẹn không hợp lệ (thuộc về quá khứ)"),
    ERR_APT_003_ALREADY_CANCELLED("ERR_APT_003_ALREADY_CANCELLED", 400, "Lịch hẹn đã bị hủy"),
    ERR_APT_004_ALREADY_CHECKED_IN("ERR_APT_004_ALREADY_CHECKED_IN", 400, "Lịch hẹn đã được tiếp nhận trước đó"),
    ERR_APT_005_NO_VET_AVAILABLE("ERR_APT_005_NO_VET_AVAILABLE", 400, "Không còn bác sĩ hoặc khung giờ phù hợp"),
    ERR_APT_006_SERVICE_NOT_FOUND("ERR_APT_006_SERVICE_NOT_FOUND", 404, "Không tìm thấy dịch vụ khám"),
    ERR_APT_007_CANNOT_CANCEL("ERR_APT_007_CANNOT_CANCEL", 400, "Không thể hủy lịch hẹn ở trạng thái hiện tại"),
    ERR_APT_008_PHONE_REQUIRED("ERR_APT_008_PHONE_REQUIRED", 400, "Cần nhập SĐT khi tạo nhanh"),
    ERR_APT_009_SLOT_FULL("ERR_APT_009_SLOT_FULL", 400, "Rất tiếc, khung giờ này phòng khám đã kín lịch. Vui lòng chọn khung giờ khác!"),

    // Boarding
    ERR_BRG_001_SESSION_NOT_FOUND("ERR_BRG_001_SESSION_NOT_FOUND", 404, "Không tìm thấy phiên lưu trú"),
    ERR_BRG_002_SESSION_NOT_ACTIVE("ERR_BRG_002_SESSION_NOT_ACTIVE", 400, "Thú cưng không còn trong thời gian lưu trú");

    private final String errorCode;
    private final int httpStatus;
    private final String message;

    ErrorCode(String errorCode, int httpStatus, String message) {
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
