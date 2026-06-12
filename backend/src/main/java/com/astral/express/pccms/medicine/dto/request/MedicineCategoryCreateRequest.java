package com.astral.express.pccms.medicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MedicineCategoryCreateRequest(
        @NotBlank @Size(max = 120) String name,
        String description,
        @NotNull Boolean isActive
) {
}
