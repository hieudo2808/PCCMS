package com.astral.express.pccms.medicalrecord.dto.response;

import com.astral.express.pccms.medicalrecord.entity.AlertSeverity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record HealthAlertResponse(
        UUID id,
        UUID petId,
        UUID medicalRecordId,
        AlertSeverity severity,
        String message,
        OffsetDateTime createdAt
) {
}
