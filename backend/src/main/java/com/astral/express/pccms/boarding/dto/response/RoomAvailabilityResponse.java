package com.astral.express.pccms.boarding.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomAvailabilityResponse(
        UUID roomTypeId,
        String roomTypeCode,
        String roomTypeName,
        Integer defaultCapacity,
        BigDecimal baseDailyPriceVnd,
        long availableRooms
) {
}
