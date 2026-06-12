package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.OffsetDateTime;

public record FinalizeMedicalRecordRequest(
    @NotBlank(message = "Chẩn đoán cuối cùng không được để trống")
    String finalDiagnosis,
    OffsetDateTime followUpAt,
    String treatmentNote
) {}
