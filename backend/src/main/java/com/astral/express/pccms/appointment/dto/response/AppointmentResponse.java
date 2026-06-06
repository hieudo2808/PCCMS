package com.astral.express.pccms.appointment.dto.response;

import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        String appointmentCode,
        AppointmentType appointmentType,
        String serviceName,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        String ownerName,
        String ownerPhone,
        UUID petId,
        String petName,
        UUID assignedVetId,
        String assignedVetName,
        AppointmentStatus statusCode,
        String statusLabel,
        String symptomText,
        String ownerNote,
        Integer queueNumber
) {}
