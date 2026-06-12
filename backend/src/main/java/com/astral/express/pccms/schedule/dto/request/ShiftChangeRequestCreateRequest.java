package com.astral.express.pccms.schedule.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ShiftChangeRequestCreateRequest(
        @NotNull
        UUID scheduleId,

        @NotBlank
        String reason,

        UUID targetStaffId
) {
}
