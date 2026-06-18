package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.appointment.service.RoomAvailabilityChecker;
import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroomingBookingUseCase {
    private final SecurityContextService securityContextService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final GroomingTicketRepository groomingTicketRepository;
    private final InvoiceRepository invoiceRepository;
    private final GroomingMapper groomingMapper;
    private final RoomAvailabilityChecker roomAvailabilityChecker;
    private final GroomingAvailabilityPolicy groomingAvailabilityPolicy;

    @Transactional
    public GroomingTicketResponse createBooking(GroomingBookingCreateRequest request) {
        UUID currentUserId = requireCurrentUserId();
        Users owner = findUser(currentUserId);
        Pets pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
        if (!pet.getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }

        ServiceCatalog service = serviceCatalogRepository
                .findByIdAndCategoryCodeAndIsActiveTrue(request.serviceId(), ServiceCategory.GROOMING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND));
        validateGroomingService(service);
        validateStartTime(request.scheduledStartAt());

        OffsetDateTime scheduledEndAt = request.scheduledStartAt().plusMinutes(service.getDurationMinutes());
        roomAvailabilityChecker.requireGroomingSlotAvailable(request.scheduledStartAt(), scheduledEndAt);
        groomingAvailabilityPolicy.requireOwnerBookingAvailable(
                owner.getId(),
                pet.getId(),
                service.getId(),
                request.scheduledStartAt(),
                scheduledEndAt
        );

        ServiceOrder serviceOrder = new ServiceOrder();
        serviceOrder.setOrderCode(generateCode("SO"));
        serviceOrder.setOwner(owner);
        serviceOrder.setPet(pet);
        serviceOrder.setService(service);
        serviceOrder.setCategoryCode(ServiceCategory.GROOMING);
        serviceOrder.setStatusCode(ServiceOrderStatus.REQUESTED);
        serviceOrder.setRequestedAt(OffsetDateTime.now());
        serviceOrder.setPlannedStartAt(request.scheduledStartAt());
        serviceOrder.setPlannedEndAt(scheduledEndAt);
        serviceOrder.setBaseAmountVnd(service.getBasePriceVnd());
        serviceOrder.setCreatedBy(owner.getId());
        serviceOrder.setUpdatedBy(owner.getId());
        ServiceOrder savedServiceOrder = serviceOrderRepository.save(serviceOrder);

        Appointment appt = new Appointment();
        appt.setServiceOrder(savedServiceOrder);
        appt.setAppointmentType(AppointmentType.GROOMING);
        appt.setScheduledStartAt(request.scheduledStartAt());
        appt.setScheduledEndAt(scheduledEndAt);
        appt.setStatusCode(AppointmentStatus.PENDING);
        appt.setOwnerNote(request.ownerNote());
        appt.setCreatedBy(owner.getId());
        Appointment appointment = appointmentRepository.save(appt);

        GroomingTicket ticketObj = new GroomingTicket();
        ticketObj.setAppointment(appointment);
        ticketObj.setStatusCode(GroomingStatus.PENDING);
        ticketObj.setOwnerNote(request.ownerNote());
        GroomingTicket ticket = groomingTicketRepository.save(ticketObj);
        log.info("[GROOMING_CREATED] - {} - {} - {}", currentUserId, ticket.getId(), OffsetDateTime.now());
        return toTicketResponse(ticket);
    }

    public PageResponse<GroomingTicketResponse> listMyTickets(Pageable pageable) {
        UUID currentUserId = requireCurrentUserId();
        return PageResponse.of(groomingTicketRepository
                .findByAppointmentServiceOrderOwnerIdOrderByAppointmentScheduledStartAtDesc(currentUserId, pageable)
                .map(this::toTicketResponse));
    }

    public GroomingTicketResponse getMyTicket(UUID ticketId) {
        GroomingTicket ticket = findTicket(ticketId);
        assertCanAccessTicket(ticket);
        return toTicketResponse(ticket);
    }

    private GroomingTicketResponse toTicketResponse(GroomingTicket ticket) {
        Invoice invoice = invoiceRepository.findByServiceOrderId(ticket.getAppointment().getServiceOrder().getId()).orElse(null);
        return groomingMapper.toTicketResponse(ticket, invoice);
    }

    private GroomingTicket findTicket(UUID ticketId) {
        return groomingTicketRepository.findWithDetailsById(ticketId)
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

    private void validateStartTime(OffsetDateTime startAt) {
        if (startAt == null || !startAt.isAfter(OffsetDateTime.now())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_003_INVALID_TIME_RANGE);
        }
    }

    private void validateGroomingService(ServiceCatalog service) {
        if (service.getDurationMinutes() == null || service.getDurationMinutes() < 1) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND);
        }
        if (service.getBasePriceVnd() == null || service.getBasePriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND);
        }
    }

    private String generateCode(String prefix) {
        return prefix + "-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}
