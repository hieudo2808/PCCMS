package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GroomingConfirmRequest(
        @NotNull UUID stationId,
        UUID assignedStaffId,
        @Size(max = 2000) String internalNote
) {
}
