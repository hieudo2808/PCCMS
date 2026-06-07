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
import com.astral.express.pccms.grooming.entity.GroomingStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface GroomingService {

    List<GroomingServiceResponse> listActiveServices();

    List<GroomingStationResponse> listActiveStations();

    GroomingTicketResponse createBooking(GroomingBookingCreateRequest request);

    PageResponse<GroomingTicketResponse> listMyTickets(Pageable pageable);

    GroomingTicketResponse getMyTicket(UUID ticketId);

    PageResponse<GroomingTicketResponse> listTickets(GroomingStatus statusCode, Pageable pageable);

    GroomingTicketResponse confirmTicket(UUID ticketId, GroomingConfirmRequest request);

    GroomingTicketResponse startTicket(UUID ticketId);

    GroomingTicketResponse completeTicket(UUID ticketId, GroomingCompleteRequest request);

    GroomingTicketResponse cancelTicket(UUID ticketId, GroomingCancelRequest request);

    List<GroomingServiceResponse> listGroomingServicesForAdmin();

    GroomingServiceResponse createGroomingService(GroomingServiceRequest request);

    GroomingServiceResponse updateGroomingService(UUID id, GroomingServiceRequest request);

    void deactivateGroomingService(UUID id);

    List<GroomingStationResponse> listStationsForAdmin();

    GroomingStationResponse createStation(GroomingStationRequest request);

    GroomingStationResponse updateStation(UUID id, GroomingStationRequest request);

    void deactivateStation(UUID id);
}
