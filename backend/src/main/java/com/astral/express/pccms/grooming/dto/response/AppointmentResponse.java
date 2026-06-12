package com.astral.express.pccms.grooming.dto.response;

import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(
        UUID id,
        UUID serviceOrderId,
        AppointmentType appointmentType,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        UUID requestedStaffId,
        UUID assignedStaffId,
        AppointmentStatus statusCode,
        UUID examRoomId,
        String symptomText,
        String ownerNote,
        String internalNote
) {
}

