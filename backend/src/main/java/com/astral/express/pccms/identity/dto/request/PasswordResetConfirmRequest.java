package com.astral.express.pccms.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(
        @NotBlank @Size(max = 255) String contact,
        @NotBlank @Size(min = 4, max = 12) String otp,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$",
                message = "Password must have at least 8 characters, including uppercase, lowercase, and digit"
        )
        String newPassword
) {
}
