package com.astral.express.pccms.medicalrecord.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicalRecordOwnerResponse(
    UUID id,
    String recordCode,
    UUID petId,
    String vetName,
    BigDecimal temperatureC,
    BigDecimal weightKg,
    Integer heartRateBpm,
    Integer respiratoryRateBpm,
    String bloodPressure,
    Integer spo2Percent,
    String mucousMembraneColor,
    BigDecimal capillaryRefillSeconds,
    String finalDiagnosis,
    String treatmentNote,
    OffsetDateTime followUpAt,
    OffsetDateTime createdAt,
    PrescriptionResponse prescription
) {}
