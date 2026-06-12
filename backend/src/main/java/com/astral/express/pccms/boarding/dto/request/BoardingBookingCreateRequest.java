package com.astral.express.pccms.boarding.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardingBookingCreateRequest(
        @NotNull UUID petId,
        @NotNull UUID roomTypeId,
        @NotNull @FutureOrPresent OffsetDateTime expectedCheckinAt,
        @NotNull OffsetDateTime expectedCheckoutAt,
        @Size(max = 2000) String specialCareRequest
) {
}
