package com.astral.express.pccms.boarding.dto.response;

import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record BoardingBookingResponse(
        UUID id,
        String bookingCode,
        UUID sessionId,
        UUID serviceOrderId,
        String orderCode,
        ServiceOrderStatus serviceOrderStatus,
        UUID ownerId,
        String ownerName,
        UUID petId,
        String petName,
        UUID requestedRoomTypeId,
        String requestedRoomTypeName,
        UUID roomId,
        String roomCode,
        String roomName,
        OffsetDateTime expectedCheckinAt,
        OffsetDateTime expectedCheckoutAt,
        OffsetDateTime actualCheckinAt,
        OffsetDateTime actualCheckoutAt,
        String specialCareRequest,
        Long estimatedPriceVnd,
        Long finalAmountVnd,
        BoardingStatus statusCode,
        InvoiceSummaryResponse invoice
) {
}

