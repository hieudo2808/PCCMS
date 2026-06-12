package com.astral.express.pccms.schedule.dto.response;

import java.time.LocalDate;
import java.util.UUID;

public record WeeklySchedulePlanItemResponse(
        UUID sourceScheduleId,
        UUID createdScheduleId,
        UUID staffId,
        String staffName,
        LocalDate sourceDate,
        LocalDate targetDate,
        UUID shiftId,
        String shiftCode,
        String shiftName,
        UUID roleId,
        String roleCode,
        Boolean conflict,
        String conflictReason
) {
}
