package com.astral.express.pccms.room.dto.response;

import com.astral.express.pccms.room.entity.RoomStatus;

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
        String description
) {
}
