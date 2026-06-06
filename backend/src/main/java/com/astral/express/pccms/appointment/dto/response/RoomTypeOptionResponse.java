package com.astral.express.pccms.appointment.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record RoomTypeOptionResponse(
        UUID id,
        String code,
        String name,
        BigDecimal baseDailyPriceVnd
) {}
