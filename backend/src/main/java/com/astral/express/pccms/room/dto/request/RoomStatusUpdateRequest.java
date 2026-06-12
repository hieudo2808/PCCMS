package com.astral.express.pccms.room.dto.request;

import com.astral.express.pccms.room.entity.RoomStatus;
import jakarta.validation.constraints.NotNull;

public record RoomStatusUpdateRequest(
        @NotNull RoomStatus statusCode
) {
}
