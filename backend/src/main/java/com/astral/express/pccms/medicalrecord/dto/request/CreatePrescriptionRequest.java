package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record CreatePrescriptionRequest(
    UUID vetId,
    String note,
    
    @NotEmpty(message = "Đơn thuốc phải có ít nhất 1 loại thuốc")
    @Valid
    List<PrescriptionItemRequest> items
) {}
