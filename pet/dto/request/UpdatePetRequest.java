package com.astral.express.pccms.pet.dto.request;

import com.astral.express.pccms.pet.entity.PetSex;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePetRequest(
    @NotBlank(message = "Tên thú cưng không được để trống")
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
    String nutritionNote
) {}
