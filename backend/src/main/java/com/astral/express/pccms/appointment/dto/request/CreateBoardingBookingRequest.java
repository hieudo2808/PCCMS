package com.astral.express.pccms.appointment.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateBoardingBookingRequest(
        @NotNull UUID petId,
        @NotNull UUID roomTypeId,
        @NotNull LocalDate checkinDate,
        @NotNull LocalDate checkoutDate,
        @Size(max = 500) String specialCareRequest
) {}
