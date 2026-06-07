package com.astral.express.pccms.medicalrecord.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

public record UpdateMedicalRecordRequest(
    @DecimalMin(value = "30.0", message = "Nhiệt độ phải lớn hơn hoặc bằng 30.0")
    @DecimalMax(value = "45.0", message = "Nhiệt độ phải nhỏ hơn hoặc bằng 45.0")
    BigDecimal temperatureC,
    
    @Min(value = 40, message = "Nhịp tim phải lớn hơn hoặc bằng 40")
    @Max(value = 250, message = "Nhịp tim phải nhỏ hơn hoặc bằng 250")
    Integer heartRateBpm,
    
    @Min(value = 10, message = "Nhịp thở phải lớn hơn hoặc bằng 10")
    @Max(value = 100, message = "Nhịp thở phải nhỏ hơn hoặc bằng 100")
    Integer respiratoryRateBpm,
    
    @DecimalMin(value = "0.1", message = "Cân nặng phải lớn hơn hoặc bằng 0.1")
    @DecimalMax(value = "100.0", message = "Cân nặng phải nhỏ hơn hoặc bằng 100.0")
    BigDecimal weightKg,
    
    String bloodPressure,
    
    @Min(value = 70, message = "SpO2 phải lớn hơn hoặc bằng 70")
    @Max(value = 100, message = "SpO2 phải nhỏ hơn hoặc bằng 100")
    Integer spo2Percent,
    
    String mucousMembraneColor,
    BigDecimal capillaryRefillSeconds,
    String preliminaryDiagnosis,
    String treatmentNote
) {}
