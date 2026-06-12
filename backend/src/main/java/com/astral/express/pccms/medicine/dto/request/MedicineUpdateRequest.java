package com.astral.express.pccms.medicine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record MedicineUpdateRequest(
        @Size(max = 60, message = "Medicine code must not exceed 60 characters")
        String medicineCode,

        @NotBlank(message = "Name is required")
        @Size(max = 160, message = "Name must not exceed 160 characters")
        String name,

        UUID categoryId,

        @NotBlank(message = "Unit is required")
        @Size(max = 40, message = "Unit must not exceed 40 characters")
        String unit,

        String defaultInstruction,

        @NotNull(message = "Current stock is required")
        @Min(value = 0, message = "Current stock must be greater than or equal to 0")
        Integer currentStock,

        @NotNull(message = "Unit price is required")
        @Min(value = 0, message = "Unit price must be greater than or equal to 0")
        Long unitPriceVnd
) {
}
