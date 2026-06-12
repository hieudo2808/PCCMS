package com.astral.express.pccms.schedule.dto.response;

import java.time.LocalTime;
import java.util.UUID;

public record ShiftOptionResponse(
        UUID id,
        String shiftCode,
        String shiftName,
        LocalTime startTime,
        LocalTime endTime
) {
}
