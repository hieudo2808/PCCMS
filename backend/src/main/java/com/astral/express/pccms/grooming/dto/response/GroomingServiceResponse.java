package com.astral.express.pccms.grooming.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record GroomingServiceResponse(
        UUID id,
        String serviceCode,
        String name,
        String description,
        BigDecimal basePriceVnd,
        Integer durationMinutes,
        Boolean isActive
) {
}
