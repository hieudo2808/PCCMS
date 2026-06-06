package com.astral.express.pccms.catalog.dto.response;

import com.astral.express.pccms.appointment.entity.ServiceCategory;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String serviceCode,
        String name,
        ServiceCategory categoryCode,
        String categoryLabel,
        String description,
        BigDecimal basePriceVnd,
        Integer durationMinutes,
        Boolean isActive
) {
}
