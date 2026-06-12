package com.astral.express.pccms.medicalrecord.dto.response;

import java.util.UUID;

public record PrescriptionItemResponse(
        UUID id,
        UUID medicineId,
        String medicineName,
        String medicineUnit,
        String dosage,
        Integer quantity,
        String instruction,
        Long unitPriceVnd
) {
}
