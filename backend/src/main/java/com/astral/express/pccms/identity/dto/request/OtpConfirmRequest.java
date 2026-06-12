package com.astral.express.pccms.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpConfirmRequest(
        @NotBlank @Size(max = 255) String contact,
        @NotBlank @Size(min = 4, max = 12) String otp
) {
}
