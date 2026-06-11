package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateGroomingAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.UpdateGroomingStatusRequest;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.service.GroomingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroomingBookingUseCase {

    private final GroomingService groomingService;
    private final GroomingTicketRepository groomingTicketRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final AppointmentResponseAssembler assembler;

    @Transactional
    public AppointmentResponse createGroomingAppointment(CreateGroomingAppointmentRequest request, UUID ownerId) {
        var createRequest = new com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest(
                request.petId(),
                serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(request.serviceCode())
                        .map(ServiceCatalog::getId)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)),
                ClinicDateTime.toOffsetDateTime(request.appointmentDate(), request.slotStart()),
                request.ownerNote()
        );
        var ticketResponse = groomingService.createBooking(createRequest);
        GroomingTicket ticket = groomingTicketRepository.findById(ticketResponse.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));
        return assembler.toResponse(ticket.getAppointment(), null);
    }

    @Transactional(readOnly = true)
    public List<GroomingBoardCardResponse> listGroomingBoard(LocalDate date) {
        LocalDate targetDate = date != null ? date : ClinicDateTime.today();
        OffsetDateTime dayStart = ClinicDateTime.startOfDay(targetDate);
        OffsetDateTime dayEnd = ClinicDateTime.endOfDay(targetDate);

        return groomingTicketRepository.findBoardForDate(dayStart, dayEnd, GroomingStatus.CANCELLED).stream()
                .map(assembler::toGroomingBoardCard)
                .toList();
    }

    @Transactional
    public GroomingBoardCardResponse updateGroomingStatus(UUID ticketId, UpdateGroomingStatusRequest request) {
        GroomingTicket ticket = groomingTicketRepository.findDetailById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));

        GroomingStatus newStatus = request.status();
        GroomingStatus current = ticket.getStatusCode();
        if (!isValidGroomingTransition(current, newStatus)) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        ticket.setStatusCode(newStatus);
        OffsetDateTime now = ClinicDateTime.now();
        if (newStatus == GroomingStatus.IN_SERVICE && ticket.getStartedAt() == null) {
            ticket.setStartedAt(now);
            ticket.getAppointment().setStatusCode(AppointmentStatus.IN_PROGRESS);
        }
        if (newStatus == GroomingStatus.COMPLETED) {
            ticket.setCompletedAt(now);
            ticket.getAppointment().setStatusCode(AppointmentStatus.COMPLETED);
            com.astral.express.pccms.appointment.entity.ServiceOrder order = ticket.getAppointment().getServiceOrder();
            order.setStatusCode(com.astral.express.pccms.appointment.entity.ServiceOrderStatus.COMPLETED);
            order.setCompletedAt(now);
        }
        if (newStatus == GroomingStatus.CANCELLED) {
            ticket.getAppointment().setStatusCode(AppointmentStatus.CANCELLED);
            com.astral.express.pccms.appointment.entity.ServiceOrder order = ticket.getAppointment().getServiceOrder();
            order.setStatusCode(com.astral.express.pccms.appointment.entity.ServiceOrderStatus.CANCELLED);
            order.setCancelledAt(now);
        }

        return assembler.toGroomingBoardCard(ticket);
    }
    private boolean isValidGroomingTransition(GroomingStatus current, GroomingStatus next) {
        if (current == GroomingStatus.CANCELLED || current == GroomingStatus.COMPLETED) {
            return false;
        }
        if (current == GroomingStatus.PENDING) {
            return next == GroomingStatus.CONFIRMED || next == GroomingStatus.CANCELLED;
        }
        if (current == GroomingStatus.CONFIRMED) {
            return next == GroomingStatus.IN_SERVICE || next == GroomingStatus.CANCELLED;
        }
        if (current == GroomingStatus.IN_SERVICE) {
            return next == GroomingStatus.COMPLETED;
        }
        return false;
    }
}
