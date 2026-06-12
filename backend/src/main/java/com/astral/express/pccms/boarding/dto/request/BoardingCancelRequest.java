package com.astral.express.pccms.boarding.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BoardingCancelRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
