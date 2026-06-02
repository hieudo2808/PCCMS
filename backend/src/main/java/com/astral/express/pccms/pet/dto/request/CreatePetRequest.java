package com.astral.express.pccms.pet.dto.request;

import com.astral.express.pccms.pet.entity.PetSex;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePetRequest(
    @NotBlank(message = "Tên thú cưng không được để trống")
    String name,
    @NotNull(message = "Loài thú cưng không được để trống")
    UUID speciesId,
    UUID breedId,
    @NotNull(message = "Giới tính thú cưng không được để trống")
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
