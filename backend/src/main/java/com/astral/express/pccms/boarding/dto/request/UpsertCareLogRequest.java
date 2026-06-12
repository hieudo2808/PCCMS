package com.astral.express.pccms.boarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record UpsertCareLogRequest(
        @NotNull(message = "Vui lòng chọn thú cưng đang lưu trú")
        UUID sessionId,
        LocalDate logDate,
        @NotBlank(message = "Vui lòng chọn buổi cập nhật")
        @Pattern(regexp = "MORNING|NOON|AFTERNOON", message = "Buổi cập nhật không hợp lệ")
        String periodCode,
        @NotBlank(message = "Vui lòng chọn tình trạng ăn uống")
        @Size(max = 120, message = "Tình trạng ăn uống quá dài")
        String feedingStatus,
        @NotBlank(message = "Vui lòng chọn tình trạng vệ sinh")
        @Size(max = 120, message = "Tình trạng vệ sinh quá dài")
        String hygieneStatus,
        @Size(max = 500, message = "Ghi chú sức khỏe quá dài")
        String healthNote,
        @Size(max = 1000, message = "Ghi chú nhân viên quá dài")
        String staffNote
) {}
