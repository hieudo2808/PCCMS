package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;

import java.time.LocalDate;
import java.util.Set;

public final class ReceptionValidation {
    private static final Set<String> CARE_PERIODS = Set.of("MORNING", "NOON", "AFTERNOON");
    private static final Set<String> GROOMING_STATUSES = Set.of("PENDING", "CONFIRMED", "IN_SERVICE", "COMPLETED", "CANCELLED");
    private static final long MAX_CARE_LOG_MEDIA_BYTES = 20L * 1024L * 1024L;

    private ReceptionValidation() {}

    public static void validateQuickAppointment(String phone, String ownerName, String petName, String symptomText) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_REC_003_PHONE_REQUIRED);
        }
        if (!phone.matches("^[0-9 .-]{9,20}$") || normalizePhone(phone).length() < 9) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (ownerName == null || ownerName.isBlank() || petName == null || petName.isBlank() || symptomText == null || symptomText.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    public static void validateCareLog(LocalDate logDate, String periodCode, String feedingStatus, String hygieneStatus) {
        if (logDate == null || logDate.isAfter(LocalDate.now())) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        if (periodCode == null || !CARE_PERIODS.contains(periodCode)) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        if (feedingStatus == null || feedingStatus.isBlank() || hygieneStatus == null || hygieneStatus.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
    }

    public static void validateCareLogMedia(long sizeBytes, String mimeType) {
        if (sizeBytes <= 0 || sizeBytes > MAX_CARE_LOG_MEDIA_BYTES) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
        if (mimeType == null || !(mimeType.startsWith("image/") || mimeType.startsWith("video/"))) {
            throw new BusinessException(ErrorCode.ERR_REC_005_INVALID_CARE_LOG);
        }
    }

    public static void validateGroomingTransition(String currentStatus, String nextStatus) {
        if (nextStatus == null || !GROOMING_STATUSES.contains(nextStatus)) {
            throw new BusinessException(ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION);
        }
        if ("COMPLETED".equals(nextStatus) && !"IN_SERVICE".equals(currentStatus)) {
            throw new BusinessException(ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION);
        }
        if ("IN_SERVICE".equals(nextStatus) && !("PENDING".equals(currentStatus) || "CONFIRMED".equals(currentStatus))) {
            throw new BusinessException(ErrorCode.ERR_REC_007_INVALID_GROOMING_STATUS_TRANSITION);
        }
    }

    private static String normalizePhone(String value) {
        return value.replaceAll("\\D", "");
    }
}
