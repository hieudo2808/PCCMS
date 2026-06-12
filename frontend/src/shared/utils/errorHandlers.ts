import { AxiosError } from "axios";
import type { ApiResponse } from "../../types";

const apiErrorMessages: Record<string, string> = {
    ERR_BOARDING_006_DUPLICATE_BOOKING:
        "Thú cưng này đã có yêu cầu lưu trú trùng khoảng thời gian. Vui lòng xem Theo dõi lưu trú hoặc chọn thời gian khác.",
    ERR_GROOMING_009_DUPLICATE_BOOKING:
        "Thú cưng này đã có yêu cầu làm đẹp trùng thời gian. Vui lòng xem Theo dõi làm đẹp hoặc chọn thời gian khác.",
    ERR_APT_001_NOT_FOUND: "Không tìm thấy lịch hẹn.",
    ERR_APT_002_PAST_DATETIME: "Không thể chọn thời gian trong quá khứ.",
    ERR_APT_003_ALREADY_CANCELLED: "Lịch hẹn đã bị hủy.",
    ERR_APT_004_ALREADY_CHECKED_IN: "Lịch hẹn đã được tiếp nhận.",
    ERR_APT_005_NO_VET_AVAILABLE: "Không có bác sĩ phù hợp trong khung giờ này.",
    ERR_APT_006_SERVICE_NOT_FOUND: "Không tìm thấy dịch vụ phù hợp.",
    ERR_APT_007_CANNOT_CANCEL: "Không thể hủy lịch hẹn ở trạng thái hiện tại.",
    ERR_APT_008_PHONE_REQUIRED: "Vui lòng nhập số điện thoại.",
    ERR_APT_009_SLOT_FULL: "Khung giờ đã hết chỗ trống.",
    ERR_MR_001: "Nhiệt độ không hợp lệ.",
    ERR_MR_002: "Nhịp tim không hợp lệ.",
    ERR_MR_003: "Nhịp thở không hợp lệ.",
    ERR_MR_004: "SpO2 không hợp lệ.",
    ERR_MR_005: "Cân nặng không hợp lệ.",
    ERR_MR_006: "Bệnh án không còn ở trạng thái nháp.",
    ERR_MR_007: "Cần nhập chẩn đoán xác định trước khi chốt bệnh án.",
    ERR_MR_008: "Cần nhập ít nhất một chỉ số sinh hiệu trước khi chốt bệnh án.",
    ERR_MED_001_RECORD_LOCKED: "Bệnh án đã khóa, không thể kê thêm thuốc.",
    ERR_MED_002_INSUFFICIENT_STOCK: "Số lượng thuốc trong kho không đủ.",
    ERR_MED_004_MEDICINE_NOT_FOUND: "Không tìm thấy thuốc được chọn.",
    ERR_BOARDING_003_INVALID_STATUS_TRANSITION: "Không thể chuyển trạng thái lưu trú ở bước hiện tại.",
    ERR_BOARDING_004_CARE_LOG_DUPLICATED: "Nhật ký cho ngày và buổi này đã tồn tại.",
    ERR_BOARDING_005_SESSION_NOT_FOUND: "Chưa có phiên lưu trú đang hoạt động.",
    ERR_GROOMING_004_INVALID_STATUS_TRANSITION: "Không thể chuyển trạng thái làm đẹp ở bước hiện tại.",
    ERR_GROOMING_005_STATION_NOT_FOUND: "Không tìm thấy khu làm đẹp.",
    ERR_GROOMING_006_STATION_UNAVAILABLE: "Khu làm đẹp không khả dụng trong khung giờ này.",
    ERR_VALIDATION_FAILED: "Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại.",
};

function formatFieldErrors(errors: unknown): string | null {
    if (!errors) return null;
    if (Array.isArray(errors)) {
        const messages = errors.filter((value): value is string => typeof value === "string" && value.trim().length > 0);
        return messages.length > 0 ? messages.join(". ") : null;
    }
    if (typeof errors === "object") {
        const messages = Object.values(errors as Record<string, unknown>).filter(
            (value): value is string => typeof value === "string" && value.trim().length > 0
        );
        return messages.length > 0 ? messages.join(". ") : null;
    }
    return null;
}

export function parseApiError(error: unknown): string {
    if (error && typeof error === "object" && "response" in error) {
        const axiosError = error as AxiosError<ApiResponse<unknown>>;
        const data = axiosError.response?.data;
        if (data && typeof data === "object") {
            const fieldErrorMessage = formatFieldErrors(data.errors);
            if (fieldErrorMessage) return fieldErrorMessage;
            if (data.errorCode && apiErrorMessages[data.errorCode]) {
                return apiErrorMessages[data.errorCode];
            }
            if (data.message) return data.message;
        }
    }
    if (error instanceof Error) {
        return error.message;
    }
    return "Đã xảy ra lỗi không xác định";
}
