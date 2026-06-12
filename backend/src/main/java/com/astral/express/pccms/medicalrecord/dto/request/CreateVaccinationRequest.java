package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public record CreateVaccinationRequest(
        @NotNull(message = "Pet ID cannot be null")
        UUID petId,
        
        UUID medicalRecordId,
        
        @NotBlank(message = "Vaccine name cannot be empty")
        String vaccineName,
        
        @NotNull(message = "Vaccination date cannot be null")
        LocalDate vaccinationDate,
        
        LocalDate nextDueDate,
        
        String note,
        
        UUID createdBy
) {
}
