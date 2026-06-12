package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.Size;

public record GroomingCompleteRequest(
        @Size(max = 2000) String internalNote
) {
}
