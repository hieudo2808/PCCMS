package com.astral.express.pccms.room.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record RoomTypeRequest(
        @NotBlank @Size(max = 60) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull @Min(1) Integer defaultCapacity,
        @NotNull @DecimalMin("0.00") BigDecimal baseDailyPriceVnd,
        String description
) {
}
