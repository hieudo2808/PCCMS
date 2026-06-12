package com.astral.express.pccms.schedule.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record WeeklySchedulePlanRequest(
        @NotNull LocalDate sourceWeekStart,
        @NotNull LocalDate targetWeekStart,
        List<UUID> roleIds,
        List<UUID> shiftIds
) {
}
