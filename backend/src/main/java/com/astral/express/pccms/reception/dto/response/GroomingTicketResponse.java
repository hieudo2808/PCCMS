package com.astral.express.pccms.reception.dto.response;

import java.util.UUID;

public record GroomingTicketResponse(
        UUID id,
        String statusCode,
        Object startedAt,
        Object completedAt,
        String ownerNote,
        String internalNote,
        UUID appointmentId,
        Object scheduledAt,
        String orderCode,
        String petName,
        String ownerName,
        String phone,
        String serviceName,
        String serviceCode
) {}
