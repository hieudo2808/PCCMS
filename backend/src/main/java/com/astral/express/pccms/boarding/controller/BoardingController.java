package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.request.BoardingBookingCreateRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingCancelRequest;
import com.astral.express.pccms.boarding.dto.request.BoardingConfirmRequest;
import com.astral.express.pccms.boarding.dto.request.CareLogCreateRequest;
import com.astral.express.pccms.boarding.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.RoomAvailabilityResponse;
import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.CarePeriod;
import com.astral.express.pccms.boarding.service.BoardingService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/boarding")
@RequiredArgsConstructor
public class BoardingController {

    private final BoardingService boardingService;

    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/availability")
    public ApiResponse<List<RoomAvailabilityResponse>> getAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt) {
        return ApiResponse.success(boardingService.getAvailability(startAt, endAt));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/bookings")
    public ApiResponse<BoardingBookingResponse> createBooking(@Valid @RequestBody BoardingBookingCreateRequest request) {
        return ApiResponse.created(boardingService.createBooking(request));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/bookings/my")
    public ApiResponse<PageResponse<BoardingBookingResponse>> listMyBookings(
            @PageableDefault(size = 10, sort = "expectedCheckinAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(boardingService.listMyBookings(pageable));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/bookings")
    public ApiResponse<PageResponse<BoardingBookingResponse>> listBookings(
            @RequestParam(required = false) BoardingStatus statusCode,
            @PageableDefault(size = 20, sort = "expectedCheckinAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(boardingService.listBookings(statusCode, pageable));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/bookings/{bookingId}/confirmations")
    public ApiResponse<BoardingBookingResponse> confirmBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody BoardingConfirmRequest request) {
        return ApiResponse.success(boardingService.confirmBooking(bookingId, request));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/bookings/{bookingId}/check-ins")
    public ApiResponse<BoardingBookingResponse> checkIn(@PathVariable UUID bookingId) {
        return ApiResponse.success(boardingService.checkIn(bookingId));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/bookings/{bookingId}/stay-starts")
    public ApiResponse<BoardingBookingResponse> startStay(@PathVariable UUID bookingId) {
        return ApiResponse.success(boardingService.startStay(bookingId));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/bookings/{bookingId}/check-outs")
    public ApiResponse<BoardingBookingResponse> checkOut(@PathVariable UUID bookingId) {
        return ApiResponse.success(boardingService.checkOut(bookingId));
    }

    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/bookings/{bookingId}/cancellations")
    public ApiResponse<BoardingBookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody BoardingCancelRequest request) {
        return ApiResponse.success(boardingService.cancelBooking(bookingId, request));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/sessions/{sessionId}/care-logs")
    public ApiResponse<CareLogResponse> createCareLog(
            @PathVariable UUID sessionId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate,
            @RequestParam CarePeriod periodCode,
            @RequestParam String feedingStatus,
            @RequestParam String hygieneStatus,
            @RequestParam(required = false) String healthNote,
            @RequestParam(required = false) String staffNote,
            @RequestParam(required = false) String caption,
            @RequestParam(required = false) List<MultipartFile> images) {
        CareLogCreateRequest request = new CareLogCreateRequest(
                logDate,
                periodCode,
                feedingStatus,
                hygieneStatus,
                healthNote,
                staffNote,
                caption);
        return ApiResponse.created(boardingService.createCareLog(sessionId, request, images));
    }

    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/bookings/{bookingId}/care-logs")
    public ApiResponse<List<CareLogResponse>> listCareLogs(@PathVariable UUID bookingId) {
        return ApiResponse.success(boardingService.listCareLogs(bookingId));
    }
}
