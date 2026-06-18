package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.entity.BoardingBooking;
import com.astral.express.pccms.boarding.entity.BoardingSession;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.RoomAllocation;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.boarding.mapper.BoardingMapper;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.BoardingSessionRepository;
import com.astral.express.pccms.boarding.repository.RoomAllocationRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardingBookingQueryService {
    private final BoardingBookingRepository boardingBookingRepository;
    private final RoomAllocationRepository roomAllocationRepository;
    private final BoardingSessionRepository boardingSessionRepository;
    private final InvoiceRepository invoiceRepository;
    private final BoardingMapper boardingMapper;

    public PageResponse<BoardingBookingResponse> listBookings(BoardingStatus statusCode, Pageable pageable) {
        if (statusCode == null) {
            return PageResponse.of(boardingBookingRepository.findAllByOrderByExpectedCheckinAtAsc(pageable)
                    .map(this::toBookingResponse));
        }
        return PageResponse.of(boardingBookingRepository
                .findByStatusCodeOrderByExpectedCheckinAtAsc(statusCode, pageable)
                .map(this::toBookingResponse));
    }

    private BoardingBookingResponse toBookingResponse(BoardingBooking booking) {
        RoomAllocation allocation = roomAllocationRepository
                .findFirstByBookingIdAndStatusCode(booking.getId(), RoomAllocationStatus.ALLOCATED)
                .orElse(null);
        BoardingSession session = boardingSessionRepository.findByBookingId(booking.getId()).orElse(null);
        Invoice invoice = invoiceRepository.findByServiceOrderId(booking.getServiceOrder().getId()).orElse(null);
        return boardingMapper.toBookingResponse(booking, allocation, session, invoice);
    }
}
