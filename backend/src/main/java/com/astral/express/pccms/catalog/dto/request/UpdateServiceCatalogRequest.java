package com.astral.express.pccms.catalog.dto.request;

import com.astral.express.pccms.appointment.entity.ServiceCategory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;



public record UpdateServiceCatalogRequest(
        @NotBlank @Size(max = 60) String serviceCode,
        @NotBlank @Size(max = 160) String name,
        @NotNull ServiceCategory categoryCode,
        String description,
        @NotNull @Min(0) Long basePriceVnd,
        @Min(1) Integer durationMinutes,
        @NotNull Boolean isActive
) {
}

