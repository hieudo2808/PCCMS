package com.astral.express.pccms.medicine.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddStockRequest(
        @NotNull(message = "Quantity to add is required")
        @Min(value = 1, message = "Quantity to add must be greater than 0")
        Integer quantityToAdd
) {
}
