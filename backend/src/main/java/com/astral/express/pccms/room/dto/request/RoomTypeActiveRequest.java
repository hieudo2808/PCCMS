package com.astral.express.pccms.room.dto.request;

import jakarta.validation.constraints.NotNull;

public record RoomTypeActiveRequest(
        @NotNull Boolean isActive
) {
}
