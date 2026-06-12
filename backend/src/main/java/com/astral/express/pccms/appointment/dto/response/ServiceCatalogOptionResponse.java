package com.astral.express.pccms.appointment.dto.response;

import com.astral.express.pccms.appointment.entity.ServiceCategory;

import java.util.UUID;

public record ServiceCatalogOptionResponse(
        UUID id,
        String serviceCode,
        String name,
        ServiceCategory categoryCode,
        Long basePriceVnd,
        Integer durationMinutes
) {}
