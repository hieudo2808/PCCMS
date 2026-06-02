package com.astral.express.pccms.medicine.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record MedicineResponse(
        UUID id,
        String medicineCode,
        String name,
        UUID categoryId,
        String categoryName,
        String unit,
        String defaultInstruction,
        Integer currentStock,
        BigDecimal unitPriceVnd,
        Boolean isActive
) {
}
