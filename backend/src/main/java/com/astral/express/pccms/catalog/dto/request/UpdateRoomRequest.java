package com.astral.express.pccms.catalog.dto.request;

import com.astral.express.pccms.catalog.entity.RoomStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateRoomRequest(
        @NotBlank @Size(max = 60) String roomCode,
        @NotBlank @Size(max = 120) String name,
        @NotNull UUID roomTypeId,
        @NotNull @Min(1) Integer capacity,
        @NotNull RoomStatus statusCode,
        @Min(1) Integer floor,
        String description
) {
}
