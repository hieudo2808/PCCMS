package com.astral.express.pccms.reception.dto.response;

import java.util.UUID;

public record BoardingBookingResponse(
        UUID id,
        String bookingCode,
        Object expectedCheckinAt,
        Object expectedCheckoutAt,
        String statusCode,
        String specialCareRequest,
        Object estimatedPriceVnd,
        String petName,
        String ownerName,
        String phone,
        String roomTypeName,
        UUID sessionId,
        String sessionStatus
) {}
