package com.astral.express.pccms.room.dto.compatibility;

import com.astral.express.pccms.room.entity.RoomStatus;

import java.util.UUID;

public record LegacyRoomResponse(
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
