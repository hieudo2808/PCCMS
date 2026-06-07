package com.astral.express.pccms.grooming.dto.response;

import com.astral.express.pccms.boarding.entity.ServiceOrderStatus;
import com.astral.express.pccms.grooming.entity.AppointmentStatus;
import com.astral.express.pccms.grooming.entity.GroomingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record GroomingTicketResponse(
        UUID id,
        UUID appointmentId,
        UUID serviceOrderId,
        String orderCode,
        ServiceOrderStatus serviceOrderStatus,
        UUID ownerId,
        String ownerName,
        UUID petId,
        String petName,
        UUID serviceId,
        String serviceCode,
        String serviceName,
        Long basePriceVnd,
        Integer durationMinutes,
        OffsetDateTime scheduledStartAt,
        OffsetDateTime scheduledEndAt,
        AppointmentStatus appointmentStatus,
        GroomingStatus statusCode,
        UUID stationId,
        String stationCode,
        String stationName,
        UUID assignedStaffId,
        String assignedStaffName,
        OffsetDateTime startedAt,
        OffsetDateTime completedAt,
        String ownerNote,
        String internalNote,
        Long estimatedAmountVnd,
        Long finalAmountVnd,
        GroomingInvoiceSummaryResponse invoice
) {
}
