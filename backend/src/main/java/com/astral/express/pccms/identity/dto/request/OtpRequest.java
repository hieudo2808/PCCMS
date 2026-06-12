package com.astral.express.pccms.identity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OtpRequest(
        @NotBlank @Size(max = 255) String contact
) {
}
