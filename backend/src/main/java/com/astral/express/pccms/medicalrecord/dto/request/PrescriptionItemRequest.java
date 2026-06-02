package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PrescriptionItemRequest(
    @NotNull(message = "ID thuốc không được để trống")
    UUID medicineId,
    
    String dosage,
    
    @NotNull(message = "Số lượng không được để trống")
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    Integer quantity,
    
    String instruction
) {}
