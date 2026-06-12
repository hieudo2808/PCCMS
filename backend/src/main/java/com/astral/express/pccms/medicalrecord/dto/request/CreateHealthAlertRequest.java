package com.astral.express.pccms.medicalrecord.dto.request;

import com.astral.express.pccms.medicalrecord.entity.AlertSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateHealthAlertRequest(
        @NotNull(message = "Pet ID cannot be null")
        UUID petId,
        
        UUID medicalRecordId,
        
        @NotNull(message = "Severity cannot be null")
        AlertSeverity severity,
        
        @NotBlank(message = "Message cannot be empty")
        String message,
        
        UUID createdBy
) {
}
