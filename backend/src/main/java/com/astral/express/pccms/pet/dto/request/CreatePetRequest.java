package com.astral.express.pccms.pet.dto.request;

import com.astral.express.pccms.pet.entity.PetSex;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePetRequest(
    /** Chỉ dùng khi lễ tân/admin đăng ký hộ; null = chủ nuôi hiện tại */
    UUID ownerId,
    @NotBlank(message = "Vui lòng nhập đầy đủ các thông tin bắt buộc")
    @Size(max = 50, message = "Tên thú cưng tối đa 50 ký tự")
    @Pattern(regexp = "^[\\p{L}0-9 ]+$", message = "Tên không chứa ký tự đặc biệt")
    String name,
    @NotNull(message = "Vui lòng nhập đầy đủ các thông tin bắt buộc")
    UUID speciesId,
    UUID breedId,
    @NotNull(message = "Vui lòng nhập đầy đủ các thông tin bắt buộc")
    PetSex sex,
    LocalDate birthDate,
    Integer estimatedAgeMonths,
    @NotNull(message = "Cân nặng phải là một con số hợp lệ")
    @DecimalMin(value = "0.01", message = "Cân nặng phải là một con số hợp lệ")
    @Digits(integer = 5, fraction = 2, message = "Cân nặng phải là một con số hợp lệ")
    BigDecimal weightKg,
    @Size(max = 50, message = "Màu lông tối đa 50 ký tự")
    String color,
    String identificationNote,
    @Size(max = 500, message = "Ghi chú đặc biệt tối đa 500 ký tự")
    String specialNote,
    String allergyNote,
    String nutritionNote
) {}
