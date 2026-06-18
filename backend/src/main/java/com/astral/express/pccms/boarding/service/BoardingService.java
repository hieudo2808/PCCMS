package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.request.CareLogCreateRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.RoomAvailabilityResponse;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.filemedia.service.MediaUploadCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardingService {

    private final BoardingBookingUseCase boardingBookingUseCase;
    private final BoardingBookingQueryService boardingBookingQueryService;
    private final BoardingStayLifecycleService boardingStayLifecycleService;
    private final BoardingCareLogApplicationService boardingCareLogApplicationService;

    public List<RoomAvailabilityResponse> getAvailability(OffsetDateTime startAt, OffsetDateTime endAt) {
        return boardingBookingUseCase.getAvailability(startAt, endAt);
    }

    @Transactional
    public BoardingBookingResponse createBooking(BoardingBookingCreateRequest request) {
        return boardingBookingUseCase.createBooking(request);
    }

    public PageResponse<BoardingBookingResponse> listMyBookings(Pageable pageable) {
        return boardingBookingUseCase.listMyBookings(pageable);
    }

    public PageResponse<BoardingBookingResponse> listBookings(BoardingStatus statusCode, Pageable pageable) {
        return boardingBookingQueryService.listBookings(statusCode, pageable);
    }

    @Transactional
    public BoardingBookingResponse confirmBooking(UUID bookingId, BoardingConfirmRequest request) {
        return boardingStayLifecycleService.confirmBooking(bookingId, request);
    }

    @Transactional
    public BoardingBookingResponse checkIn(UUID bookingId) {
        return boardingStayLifecycleService.checkIn(bookingId);
    }

    @Transactional
    public BoardingBookingResponse startStay(UUID bookingId) {
        return boardingStayLifecycleService.startStay(bookingId);
    }

    @Transactional
    public BoardingBookingResponse checkOut(UUID bookingId) {
        return boardingStayLifecycleService.checkOut(bookingId);
    }

    @Transactional
    public BoardingBookingResponse cancelBooking(UUID bookingId, BoardingCancelRequest request) {
        return boardingStayLifecycleService.cancelBooking(bookingId, request);
    }

    @Transactional
    public CareLogResponse createCareLog(
            UUID sessionId,
            CareLogCreateRequest request,
            List<MediaUploadCommand> images) {
        return boardingCareLogApplicationService.createCareLog(sessionId, request, images);
    }

    public List<CareLogResponse> listCareLogs(UUID bookingId) {
        return boardingCareLogApplicationService.listCareLogs(bookingId);
    }

}



