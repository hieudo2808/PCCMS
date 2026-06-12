package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateLabResultRequest(
        @NotBlank @Size(max = 160) String testName,
        String resultText,
        UUID fileId
) {
}
