package com.astral.express.pccms.medicine.dto.response;

import java.util.UUID;

public record MedicineCategoryResponse(
        UUID id,
        String name,
        String description,
        Boolean isActive
) {
}
