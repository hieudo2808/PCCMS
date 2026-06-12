package com.astral.express.pccms.reception.dto.response;

import java.util.UUID;

public record AppointmentReceptionResponse(
        UUID id,
        String statusCode,
        Object scheduledStartAt,
        Object scheduledEndAt,
        String symptomText,
        String orderCode,
        String ownerName,
        String phone,
        String petName,
        String doctorName,
        String serviceName
) {}
