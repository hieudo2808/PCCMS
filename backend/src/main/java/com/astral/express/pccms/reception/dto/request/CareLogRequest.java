package com.astral.express.pccms.reception.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CareLogRequest(
        UUID sessionId,
        UUID petId,
        String logDate,
        @NotBlank(message = "Vui lòng chọn buổi cập nhật")
        @Pattern(regexp = "MORNING|NOON|AFTERNOON", message = "Buổi cập nhật phải là MORNING, NOON hoặc AFTERNOON")
        String periodCode,
        @NotBlank(message = "Vui lòng chọn trạng thái ăn uống")
        @Size(max = 120)
        String feedingStatus,
        @NotBlank(message = "Vui lòng chọn trạng thái vệ sinh")
        @Size(max = 120)
        String hygieneStatus,
        @Size(max = 500)
        String healthNote,
        @Size(max = 500)
        String staffNote
) {}
