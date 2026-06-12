package com.astral.express.pccms.schedule.dto.response;

import com.astral.express.pccms.schedule.entity.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record WorkScheduleResponse(
        UUID id,
        UUID staffId,
        String staffName,
        LocalDate workDate,
        UUID shiftId,
        String shiftCode,
        String shiftName,
        LocalTime startTime,
        LocalTime endTime,
        UUID examRoomId,
        String examRoomCode,
        String examRoomName,
        UUID stationId,
        String stationCode,
        String stationName,
        UUID roleId,
        String roleCode,
        Integer capacity,
        ScheduleStatus statusCode,
        String note
) {
}
