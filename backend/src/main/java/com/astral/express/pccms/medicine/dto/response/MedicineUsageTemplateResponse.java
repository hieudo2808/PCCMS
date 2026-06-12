package com.astral.express.pccms.medicine.dto.response;

import java.util.UUID;

public record MedicineUsageTemplateResponse(
        UUID id,
        UUID medicineId,
        String label,
        String dosage,
        String frequency,
        Integer durationDays,
        String instruction,
        Boolean isDefault,
        Integer sortOrder,
        Boolean isActive
) {}
