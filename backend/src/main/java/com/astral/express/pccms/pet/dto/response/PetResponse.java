package com.astral.express.pccms.pet.dto.response;

import com.astral.express.pccms.pet.entity.PetSex;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PetResponse(
    UUID id,
    UUID ownerId,
    String name,
    UUID speciesId,
    UUID breedId,
    PetSex sex,
    LocalDate birthDate,
    Integer estimatedAgeMonths,
    BigDecimal weightKg,
    String color,
    String identificationNote,
    String specialNote,
    String allergyNote,
    String nutritionNote,
    Boolean isActive,
    java.util.List<com.astral.express.pccms.medicalrecord.dto.response.HealthAlertResponse> healthAlerts
) {}
