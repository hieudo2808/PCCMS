package com.astral.express.pccms.catalog.dto.response;

import com.astral.express.pccms.catalog.entity.RoomStatus;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String roomCode,
        String name,
        UUID roomTypeId,
        String roomTypeName,
        Integer floor,
        Integer capacity,
        RoomStatus statusCode,
        String statusLabel,
        String description
) {
}
