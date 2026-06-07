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
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BoardingService {
    List<RoomAvailabilityResponse> getAvailability(OffsetDateTime startAt, OffsetDateTime endAt);

    BoardingBookingResponse createBooking(BoardingBookingCreateRequest request);

    PageResponse<BoardingBookingResponse> listMyBookings(Pageable pageable);

    PageResponse<BoardingBookingResponse> listBookings(BoardingStatus statusCode, Pageable pageable);

    BoardingBookingResponse confirmBooking(UUID bookingId, BoardingConfirmRequest request);

    BoardingBookingResponse checkIn(UUID bookingId);

    BoardingBookingResponse startStay(UUID bookingId);

    BoardingBookingResponse checkOut(UUID bookingId);

    BoardingBookingResponse cancelBooking(UUID bookingId, BoardingCancelRequest request);

    CareLogResponse createCareLog(UUID sessionId, CareLogCreateRequest request, List<MultipartFile> images);

    List<CareLogResponse> listCareLogs(UUID bookingId);
}
