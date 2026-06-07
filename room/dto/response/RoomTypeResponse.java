package com.astral.express.pccms.room.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        String code,
        String name,
        Integer defaultCapacity,
        Long baseDailyPriceVnd,
        String description,
        Boolean isActive
) {
}
