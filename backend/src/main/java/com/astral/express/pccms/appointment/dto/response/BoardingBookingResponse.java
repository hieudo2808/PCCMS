package com.astral.express.pccms.appointment.dto.response;

import com.astral.express.pccms.appointment.entity.BoardingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardingBookingResponse(
        UUID id,
        String bookingCode,
        UUID petId,
        String petName,
        String roomTypeName,
        OffsetDateTime expectedCheckinAt,
        OffsetDateTime expectedCheckoutAt,
        BigDecimal estimatedPriceVnd,
        BoardingStatus statusCode,
        String statusLabel,
        String specialCareRequest
) {}
