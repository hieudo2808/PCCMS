package com.astral.express.pccms.medicine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

public record CreateMedicineUsageTemplateRequest(
        @NotBlank @Size(max = 120) String label,
        @Size(max = 120) String dosage,
        @Size(max = 120) String frequency,
        @Min(0) Integer durationDays,
        @NotBlank String instruction,
        @NotNull Boolean isDefault,
        @NotNull Integer sortOrder
) {}
