package com.astral.express.pccms.catalog.dto.response;

import com.astral.express.pccms.appointment.entity.ServiceCategory;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ServiceCatalogResponse(
        UUID id,
        String serviceCode,
        String name,
        ServiceCategory categoryCode,
        String description,
        Long basePriceVnd,
        Integer durationMinutes,
        Boolean isActive,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

