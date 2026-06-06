package com.astral.express.pccms.appointment.dto.response;

import com.astral.express.pccms.appointment.entity.GroomingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record GroomingBoardCardResponse(
        UUID ticketId,
        UUID appointmentId,
        String petName,
        String serviceName,
        OffsetDateTime scheduledStartAt,
        GroomingStatus statusCode,
        String statusLabel,
        String stationName
) {}
