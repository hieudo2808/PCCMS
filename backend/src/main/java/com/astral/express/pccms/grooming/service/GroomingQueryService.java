package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroomingQueryService {
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final GroomingTicketRepository groomingTicketRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final InvoiceRepository invoiceRepository;
    private final GroomingMapper groomingMapper;

    public List<GroomingServiceResponse> listActiveServices() {
        return serviceCatalogRepository
                .findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING)
                .stream()
                .map(groomingMapper::toServiceResponse)
                .toList();
    }

    public List<GroomingStationResponse> listActiveStations() {
        return groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc().stream()
                .map(groomingMapper::toStationResponse)
                .toList();
    }

    public PageResponse<GroomingTicketResponse> listTickets(GroomingStatus statusCode, Pageable pageable) {
        if (statusCode == null) {
            return PageResponse.of(groomingTicketRepository
                    .findAllByOrderByAppointmentScheduledStartAtAsc(pageable)
                    .map(this::toTicketResponse));
        }
        return PageResponse.of(groomingTicketRepository
                .findByStatusCodeOrderByAppointmentScheduledStartAtAsc(statusCode, pageable)
                .map(this::toTicketResponse));
    }

    private GroomingTicketResponse toTicketResponse(GroomingTicket ticket) {
        Invoice invoice = invoiceRepository
                .findByServiceOrderId(ticket.getAppointment().getServiceOrder().getId())
                .orElse(null);
        return groomingMapper.toTicketResponse(ticket, invoice);
    }
}
