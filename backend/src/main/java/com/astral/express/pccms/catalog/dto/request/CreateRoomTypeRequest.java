package com.astral.express.pccms.catalog.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRoomTypeRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) Integer defaultCapacity,
        @NotNull @Min(0) BigDecimal baseDailyPriceVnd,
        String description,
        @NotNull Boolean isActive
) {
}
