package com.astral.express.pccms.medicalrecord.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LabResultResponse(
        UUID id,
        UUID medicalRecordId,
        String testName,
        String resultText,
        UUID fileId,
        UUID createdBy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
