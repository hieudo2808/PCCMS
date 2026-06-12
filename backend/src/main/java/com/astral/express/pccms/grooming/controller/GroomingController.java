package com.astral.express.pccms.grooming.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCancelRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.grooming.service.GroomingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/grooming")
@RequiredArgsConstructor
public class GroomingController {

    private final GroomingService groomingService;

    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/services")
    public ApiResponse<List<GroomingServiceResponse>> listActiveServices() {
        return ApiResponse.success(groomingService.listActiveServices());
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/stations")
    public ApiResponse<List<GroomingStationResponse>> listActiveStations() {
        return ApiResponse.success(groomingService.listActiveStations());
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/tickets")
    public ApiResponse<GroomingTicketResponse> createBooking(@Valid @RequestBody GroomingBookingCreateRequest request) {
        return ApiResponse.created(groomingService.createBooking(request));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/tickets/my")
    public ApiResponse<PageResponse<GroomingTicketResponse>> listMyTickets(
            @PageableDefault(size = 10, sort = "appointment.scheduledStartAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(groomingService.listMyTickets(pageable));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping("/tickets/my/{ticketId}")
    public ApiResponse<GroomingTicketResponse> getMyTicket(@PathVariable UUID ticketId) {
        return ApiResponse.success(groomingService.getMyTicket(ticketId));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @GetMapping("/tickets")
    public ApiResponse<PageResponse<GroomingTicketResponse>> listTickets(
            @RequestParam(required = false) GroomingStatus statusCode,
            @PageableDefault(size = 20, sort = "appointment.scheduledStartAt", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(groomingService.listTickets(statusCode, pageable));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/tickets/{ticketId}/confirmations")
    public ApiResponse<GroomingTicketResponse> confirmTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody GroomingConfirmRequest request) {
        return ApiResponse.success(groomingService.confirmTicket(ticketId, request));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/tickets/{ticketId}/starts")
    public ApiResponse<GroomingTicketResponse> startTicket(@PathVariable UUID ticketId) {
        return ApiResponse.success(groomingService.startTicket(ticketId));
    }

    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/tickets/{ticketId}/completions")
    public ApiResponse<GroomingTicketResponse> completeTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody GroomingCompleteRequest request) {
        return ApiResponse.success(groomingService.completeTicket(ticketId, request));
    }

    @PreAuthorize("hasRole('OWNER') or hasRole('STAFF') or hasRole('ADMIN')")
    @PostMapping("/tickets/{ticketId}/cancellations")
    public ApiResponse<GroomingTicketResponse> cancelTicket(
            @PathVariable UUID ticketId,
            @Valid @RequestBody GroomingCancelRequest request) {
        return ApiResponse.success(groomingService.cancelTicket(ticketId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/services")
    public ApiResponse<List<GroomingServiceResponse>> listGroomingServicesForAdmin() {
        return ApiResponse.success(groomingService.listGroomingServicesForAdmin());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/services")
    public ApiResponse<GroomingServiceResponse> createGroomingService(@Valid @RequestBody GroomingServiceRequest request) {
        return ApiResponse.created(groomingService.createGroomingService(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/services/{id}")
    public ApiResponse<GroomingServiceResponse> updateGroomingService(
            @PathVariable UUID id,
            @Valid @RequestBody GroomingServiceRequest request) {
        return ApiResponse.success(groomingService.updateGroomingService(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/admin/services/{id}")
    public ApiResponse<Void> deactivateGroomingService(@PathVariable UUID id) {
        groomingService.deactivateGroomingService(id);
        return ApiResponse.success(null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/stations")
    public ApiResponse<List<GroomingStationResponse>> listStationsForAdmin() {
        return ApiResponse.success(groomingService.listStationsForAdmin());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/stations")
    public ApiResponse<GroomingStationResponse> createStation(@Valid @RequestBody GroomingStationRequest request) {
        return ApiResponse.created(groomingService.createStation(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/admin/stations/{id}")
    public ApiResponse<GroomingStationResponse> updateStation(
            @PathVariable UUID id,
            @Valid @RequestBody GroomingStationRequest request) {
        return ApiResponse.success(groomingService.updateStation(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/admin/stations/{id}/deactivation")
    public ApiResponse<Void> deactivateStation(@PathVariable UUID id) {
        groomingService.deactivateStation(id);
        return ApiResponse.success(null);
    }
}

