package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateMedicalRecordRequest(
        @DecimalMin(value = "0.0", message = "Temperature must not be negative")
        BigDecimal temperatureC,

        @Min(value = 0, message = "Heart rate must not be negative")
        Integer heartRateBpm,

        @Min(value = 0, message = "Respiratory rate must not be negative")
        Integer respiratoryRateBpm,

        @DecimalMin(value = "0.0", message = "Weight must not be negative")
        BigDecimal weightKg,

        String bloodPressure,

        @Min(value = 0, message = "SpO2 must be greater than or equal to 0")
        @Max(value = 100, message = "SpO2 must be less than or equal to 100")
        Integer spo2Percent,

        String mucousMembraneColor,

        @DecimalMin(value = "0.0", message = "Capillary refill time must not be negative")
        BigDecimal capillaryRefillSeconds,

        String preliminaryDiagnosis,
        String treatmentNote
) {
}
