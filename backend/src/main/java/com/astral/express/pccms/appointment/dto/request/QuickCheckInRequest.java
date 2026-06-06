package com.astral.express.pccms.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record QuickCheckInRequest(
        @NotBlank
        @Pattern(regexp = "^[0-9\\s.\\-]+$", message = "Số điện thoại không hợp lệ")
        String phone,
        @NotNull UUID petId,
        UUID assignedVetId,
        @Size(max = 500) String symptomText
) {}
