package com.astral.express.pccms.schedule.dto.response;

import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ShiftChangeRequestResponse(
        UUID id,
        UUID scheduleId,
        String requestedBy,
        String targetStaff,
        OffsetDateTime workDate,
        String shiftName,
        String reason,
        ShiftRequestStatus statusCode,
        String resolvedBy,
        OffsetDateTime resolvedAt,
        OffsetDateTime createdAt
) {
}
