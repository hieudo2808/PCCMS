package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GroomingServiceRequest(
        @NotBlank @Size(max = 60) String serviceCode,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0") BigDecimal basePriceVnd,
        @NotNull @Min(1) Integer durationMinutes
) {
}
