package com.astral.express.pccms.schedule.dto.request;

import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import jakarta.validation.constraints.NotNull;

public record ShiftRequestStatusUpdateRequest(
        @NotNull
        ShiftRequestStatus statusCode
) {
}
