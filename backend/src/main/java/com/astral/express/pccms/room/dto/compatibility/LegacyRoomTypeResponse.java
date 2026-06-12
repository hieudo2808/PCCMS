package com.astral.express.pccms.room.dto.compatibility;

import java.math.BigDecimal;
import java.util.UUID;

public record LegacyRoomTypeResponse(
        UUID id,
        String code,
        String name,
        Integer defaultCapacity,
        BigDecimal baseDailyPriceVnd,
        String description,
        Boolean isActive
) {
}
