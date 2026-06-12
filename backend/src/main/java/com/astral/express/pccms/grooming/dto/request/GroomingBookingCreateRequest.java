package com.astral.express.pccms.grooming.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GroomingBookingCreateRequest(
        @NotNull UUID petId,
        @NotNull UUID serviceId,
        @NotNull @Future OffsetDateTime scheduledStartAt,
        @Size(max = 2000) String ownerNote
) {
}
