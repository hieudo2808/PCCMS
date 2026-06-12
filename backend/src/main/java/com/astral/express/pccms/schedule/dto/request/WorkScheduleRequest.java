package com.astral.express.pccms.schedule.dto.request;

import com.astral.express.pccms.schedule.entity.ScheduleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record WorkScheduleRequest(
        @NotNull
        UUID staffId,

        @NotNull
        LocalDate workDate,

        @NotNull
        UUID shiftId,

        UUID examRoomId,

        UUID stationId,

        @NotNull
        UUID roleId,

        @NotNull
        @Min(1)
        Integer capacity,

        @NotNull
        ScheduleStatus statusCode,

        String note
) {
}
