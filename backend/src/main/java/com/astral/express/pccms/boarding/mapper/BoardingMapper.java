package com.astral.express.pccms.boarding.mapper;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogMediaResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.InvoiceSummaryResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.CareLog;
import com.astral.express.pccms.boarding.entity.CareLogMedia;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardingMapper {

    public BoardingBookingResponse toBookingResponse(
            BoardingBooking booking,
            RoomAllocation allocation,
            BoardingSession session,
            Invoice invoice) {
        return new BoardingBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                session == null ? null : session.getId(),
                booking.getServiceOrder().getId(),
                booking.getServiceOrder().getOrderCode(),
                booking.getServiceOrder().getStatusCode(),
                booking.getOwner().getId(),
                booking.getOwner().getFullName(),
                booking.getPet().getId(),
                booking.getPet().getName(),
                booking.getRequestedRoomType().getId(),
                booking.getRequestedRoomType().getName(),
                allocation == null ? null : allocation.getRoom().getId(),
                allocation == null ? null : allocation.getRoom().getRoomCode(),
                allocation == null ? null : allocation.getRoom().getName(),
                booking.getExpectedCheckinAt(),
                booking.getExpectedCheckoutAt(),
                session == null ? null : session.getActualCheckinAt(),
                session == null ? null : session.getActualCheckoutAt(),
                booking.getSpecialCareRequest(),
                booking.getEstimatedPriceVnd(),
                booking.getServiceOrder().getFinalAmountVnd() == null ? null : booking.getServiceOrder().getFinalAmountVnd().longValue(),
                booking.getStatusCode(),
                toInvoiceSummary(invoice));
    }

    public CareLogResponse toCareLogResponse(CareLog careLog, List<CareLogMedia> media) {
        return new CareLogResponse(
                careLog.getId(),
                careLog.getSession().getId(),
                careLog.getLogDate(),
                careLog.getPeriodCode(),
                careLog.getFeedingStatus(),
                careLog.getHygieneStatus(),
                careLog.getHealthNote(),
                careLog.getStaffNote(),
                careLog.getStaff().getId(),
                careLog.getStaff().getFullName(),
                careLog.getCreatedAt(),
                media.stream().map(this::toCareLogMediaResponse).toList());
    }

    private CareLogMediaResponse toCareLogMediaResponse(CareLogMedia media) {
        return new CareLogMediaResponse(
                media.getId(),
                media.getFile().getId(),
                media.getFile().getStoredKey(),
                media.getFile().getMimeType(),
                media.getCaption());
    }

    private InvoiceSummaryResponse toInvoiceSummary(Invoice invoice) {
        if (invoice == null) {
            return null;
        }
        return new InvoiceSummaryResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getStatusCode(),
                invoice.getTotalAmountVnd(),
                invoice.getPaidAmountVnd(),
                invoice.getIssuedAt());
    }
}
