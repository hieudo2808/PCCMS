package com.astral.express.pccms.catalog.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeResponse(
        UUID id,
        String code,
        String name,
        Integer defaultCapacity,
        BigDecimal baseDailyPriceVnd,
        String description,
        Boolean isActive
) {
}
