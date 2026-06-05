package com.astral.express.pccms.pet.dto.response;

import com.astral.express.pccms.pet.entity.PetSex;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Hồ sơ nền thú cưng — dùng chung cho các subsystem (khám, làm đẹp, lưu trú, hóa đơn).
 * Không chứa dữ liệu y tế theo từng lần khám; cảnh báo sức khỏe thuộc medicalrecord.
 */
public record PetResponse(
        UUID id,
        UUID ownerId,
        String name,
        UUID speciesId,
        String speciesName,
        UUID breedId,
        String breedName,
        PetSex sex,
        LocalDate birthDate,
        Integer estimatedAgeMonths,
        BigDecimal weightKg,
        String color,
        String identificationNote,
        String specialNote,
        String allergyNote,
        String nutritionNote,
        Boolean isActive
) {}
