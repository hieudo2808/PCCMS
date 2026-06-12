package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record GroomingStationRequest(
        @NotBlank @Size(max = 20) String stationCode,
        @NotBlank @Size(max = 80) String name,
        @NotNull Boolean isActive
) {
}
