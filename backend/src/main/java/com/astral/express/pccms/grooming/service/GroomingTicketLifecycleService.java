package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingCancelRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroomingTicketLifecycleService {
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final GroomingTicketRepository groomingTicketRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final BillingHandoffService billingHandoffService;
    private final InvoiceRepository invoiceRepository;
    private final GroomingMapper groomingMapper;
    private final GroomingAvailabilityPolicy groomingAvailabilityPolicy;

    @Transactional
    public GroomingTicketResponse confirmTicket(UUID ticketId, GroomingConfirmRequest request) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);
        GroomingStation station = groomingStationRepository.findWithLockById(request.stationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        if (!Boolean.TRUE.equals(station.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND);
        }
        groomingAvailabilityPolicy.requireStationAvailable(
                station.getId(),
                ticket.getId(),
                ticket.getAppointment().getScheduledStartAt(),
                ticket.getAppointment().getScheduledEndAt()
        );

        Users assignedStaff = request.assignedStaffId() == null ? actor : findUser(request.assignedStaffId());

        ticket.confirm(station, assignedStaff, request.internalNote(), actor.getId());

        appointmentRepository.save(ticket.getAppointment());
        serviceOrderRepository.save(ticket.getAppointment().getServiceOrder());
        return toTicketResponse(groomingTicketRepository.save(ticket));
    }

    @Transactional
    public GroomingTicketResponse startTicket(UUID ticketId) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);

        ticket.start(OffsetDateTime.now(), actor.getId());

        appointmentRepository.save(ticket.getAppointment());
        serviceOrderRepository.save(ticket.getAppointment().getServiceOrder());
        return toTicketResponse(groomingTicketRepository.save(ticket));
    }

    @Transactional
    public GroomingTicketResponse completeTicket(UUID ticketId, GroomingCompleteRequest request) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);

        if (ticket.getStatusCode() == GroomingStatus.COMPLETED) {
            return toTicketResponse(ticket);
        }

        ticket.complete(OffsetDateTime.now(), request.internalNote(), actor.getId());

        Appointment appointment = ticket.getAppointment();
        ServiceOrder serviceOrder = appointment.getServiceOrder();
        appointmentRepository.save(appointment);
        serviceOrderRepository.save(serviceOrder);
        GroomingTicket savedTicket = groomingTicketRepository.save(ticket);
        billingHandoffService.createGroomingInvoice(serviceOrder, appointment, savedTicket, actor);
        return toTicketResponse(savedTicket);
    }

    @Transactional
    public GroomingTicketResponse cancelTicket(UUID ticketId, GroomingCancelRequest request) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);
        assertCanAccessTicket(ticket);

        boolean isStaff = securityContextService.hasAnyRole("ADMIN", "STAFF");
        ticket.cancel(request.reason(), OffsetDateTime.now(), actor.getId(), isStaff);

        appointmentRepository.save(ticket.getAppointment());
        serviceOrderRepository.save(ticket.getAppointment().getServiceOrder());
        return toTicketResponse(groomingTicketRepository.save(ticket));
    }

    private GroomingTicketResponse toTicketResponse(GroomingTicket ticket) {
        Invoice invoice = invoiceRepository.findByServiceOrderId(ticket.getAppointment().getServiceOrder().getId()).orElse(null);
        return groomingMapper.toTicketResponse(ticket, invoice);
    }

    private GroomingTicket findLockedTicket(UUID ticketId) {
        return groomingTicketRepository.findLockedWithDetailsById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_001_TICKET_NOT_FOUND));
    }

    private Users findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void assertCanAccessTicket(GroomingTicket ticket) {
        if (securityContextService.hasAnyRole("ADMIN", "STAFF")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        if (!ticket.getAppointment().getServiceOrder().getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }
}
