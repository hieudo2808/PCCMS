package com.astral.express.pccms.medicalrecord.dto.response;

import com.astral.express.pccms.medicalrecord.entity.RecordStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicalRecordResponse(
    UUID id,
    String recordCode,
    UUID appointmentId,
    UUID petId,
    String petName,
    UUID vetId,
    String vetName,
    RecordStatus recordStatus,
    BigDecimal temperatureC,
    Integer heartRateBpm,
    Integer respiratoryRateBpm,
    BigDecimal weightKg,
    String bloodPressure,
    Integer spo2Percent,
    String mucousMembraneColor,
    BigDecimal capillaryRefillSeconds,
    String preliminaryDiagnosis,
    String finalDiagnosis,
    String treatmentNote,
    OffsetDateTime followUpAt,
    OffsetDateTime lockedAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
