package com.astral.express.pccms.appointment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateMedicalAppointmentRequest(
        @NotNull UUID petId,
        @NotNull LocalDate appointmentDate,
        @NotNull LocalTime slotStart,
        UUID requestedVetId,
        @NotBlank @Size(max = 500) String symptomText,
        @Size(max = 255) String ownerNote
) {}
