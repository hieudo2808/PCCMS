package com.astral.express.pccms.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateGroomingAppointmentRequest(
        @NotNull UUID petId,
        @NotBlank String serviceCode,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime slotStart,
        @Size(max = 255) String ownerNote
) {}
