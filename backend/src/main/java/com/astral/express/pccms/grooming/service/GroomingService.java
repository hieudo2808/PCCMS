package com.astral.express.pccms.grooming.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroomingService {

    private final GroomingQueryService groomingQueryService;
    private final GroomingBookingUseCase groomingBookingUseCase;
    private final GroomingTicketLifecycleService groomingTicketLifecycleService;
    private final GroomingCatalogAdminService groomingCatalogAdminService;
    private final GroomingStationAdminService groomingStationAdminService;

    public List<GroomingServiceResponse> listActiveServices() {
        return groomingQueryService.listActiveServices();
    }

    public List<GroomingStationResponse> listActiveStations() {
        return groomingQueryService.listActiveStations();
    }

    @Transactional
    public GroomingTicketResponse createBooking(GroomingBookingCreateRequest request) {
        return groomingBookingUseCase.createBooking(request);
    }

    public PageResponse<GroomingTicketResponse> listMyTickets(Pageable pageable) {
        return groomingBookingUseCase.listMyTickets(pageable);
    }

    public GroomingTicketResponse getMyTicket(UUID ticketId) {
        return groomingBookingUseCase.getMyTicket(ticketId);
    }

    public PageResponse<GroomingTicketResponse> listTickets(GroomingStatus statusCode, Pageable pageable) {
        return groomingQueryService.listTickets(statusCode, pageable);
    }

    @Transactional
    public GroomingTicketResponse confirmTicket(UUID ticketId, GroomingConfirmRequest request) {
        return groomingTicketLifecycleService.confirmTicket(ticketId, request);
    }

    @Transactional
    public GroomingTicketResponse startTicket(UUID ticketId) {
        return groomingTicketLifecycleService.startTicket(ticketId);
    }

    @Transactional
    public GroomingTicketResponse completeTicket(UUID ticketId, GroomingCompleteRequest request) {
        return groomingTicketLifecycleService.completeTicket(ticketId, request);
    }

    @Transactional
    public GroomingTicketResponse cancelTicket(UUID ticketId, GroomingCancelRequest request) {
        return groomingTicketLifecycleService.cancelTicket(ticketId, request);
    }

    public List<GroomingServiceResponse> listGroomingServicesForAdmin() {
        return groomingCatalogAdminService.listGroomingServicesForAdmin();
    }

    @Transactional
    public GroomingServiceResponse createGroomingService(GroomingServiceRequest request) {
        return groomingCatalogAdminService.createGroomingService(request);
    }

    @Transactional
    public GroomingServiceResponse updateGroomingService(UUID id, GroomingServiceRequest request) {
        return groomingCatalogAdminService.updateGroomingService(id, request);
    }

    @Transactional
    public void deactivateGroomingService(UUID id) {
        groomingCatalogAdminService.deactivateGroomingService(id);
    }

    public List<GroomingStationResponse> listStationsForAdmin() {
        return groomingStationAdminService.listStationsForAdmin();
    }

    @Transactional
    public GroomingStationResponse createStation(GroomingStationRequest request) {
        return groomingStationAdminService.createStation(request);
    }

    @Transactional
    public GroomingStationResponse updateStation(UUID id, GroomingStationRequest request) {
        return groomingStationAdminService.updateStation(id, request);
    }

    @Transactional
    public void deactivateStation(UUID id) {
        groomingStationAdminService.deactivateStation(id);
    }

}
