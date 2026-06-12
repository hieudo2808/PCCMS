package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GroomingCancelRequest(
        @NotBlank @Size(max = 500) String reason
) {
}
