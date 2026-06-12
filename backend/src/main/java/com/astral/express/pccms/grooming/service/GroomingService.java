package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.billing.entity.Invoice;
import com.astral.express.pccms.billing.repository.InvoiceRepository;
import com.astral.express.pccms.billing.service.BillingHandoffService;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.dto.request.GroomingBookingCreateRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCancelRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingCompleteRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingConfirmRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingServiceRequest;
import com.astral.express.pccms.grooming.dto.request.GroomingStationRequest;
import com.astral.express.pccms.grooming.dto.response.GroomingServiceResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingStationResponse;
import com.astral.express.pccms.grooming.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.entity.GroomingTicket;
import com.astral.express.pccms.grooming.mapper.GroomingMapper;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.appointment.service.RoomAvailabilityChecker;
import com.astral.express.pccms.grooming.service.GroomingService;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GroomingService {

    private static final List<GroomingStatus> STATION_BLOCKING_STATUSES = List.of(
            GroomingStatus.CONFIRMED,
            GroomingStatus.IN_SERVICE);
    private static final List<GroomingStatus> OWNER_DUPLICATE_BLOCKING_STATUSES = List.of(
            GroomingStatus.PENDING,
            GroomingStatus.CONFIRMED,
            GroomingStatus.IN_SERVICE);

    private final SecurityContextService SecurityContextService;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final GroomingTicketRepository groomingTicketRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final BillingHandoffService billingHandoffService;
    private final InvoiceRepository invoiceRepository;
    private final GroomingMapper groomingMapper;
    private final RoomAvailabilityChecker roomAvailabilityChecker;
public List<GroomingServiceResponse> listActiveServices() {
        return serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING).stream()
                .map(groomingMapper::toServiceResponse)
                .toList();
    }
public List<GroomingStationResponse> listActiveStations() {
        return groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc().stream()
                .map(groomingMapper::toStationResponse)
                .toList();
    }
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
        ensureOwnerBookingNotDuplicated(owner.getId(), pet.getId(), service.getId(), request.scheduledStartAt(), scheduledEndAt);
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
public PageResponse<GroomingTicketResponse> listTickets(GroomingStatus statusCode, Pageable pageable) {
        if (statusCode == null) {
            return PageResponse.of(groomingTicketRepository.findAllByOrderByAppointmentScheduledStartAtAsc(pageable)
                    .map(this::toTicketResponse));
        }
        return PageResponse.of(groomingTicketRepository.findByStatusCodeOrderByAppointmentScheduledStartAtAsc(statusCode, pageable)
                .map(this::toTicketResponse));
    }
@Transactional
    public GroomingTicketResponse confirmTicket(UUID ticketId, GroomingConfirmRequest request) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);
        GroomingStation station = groomingStationRepository.findWithLockById(request.stationId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        if (!Boolean.TRUE.equals(station.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND);
        }
        ensureStationAvailable(station.getId(), ticket, ticket.getAppointment().getScheduledStartAt(), ticket.getAppointment().getScheduledEndAt());

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
        
        appointmentRepository.save(ticket.getAppointment());
        serviceOrderRepository.save(ticket.getAppointment().getServiceOrder());
        GroomingTicket savedTicket = groomingTicketRepository.save(ticket);
        billingHandoffService.createGroomingInvoice(
                savedTicket.getAppointment().getServiceOrder(),
                savedTicket.getAppointment(),
                savedTicket,
                actor);
        return toTicketResponse(savedTicket);
    }
@Transactional
    public GroomingTicketResponse cancelTicket(UUID ticketId, GroomingCancelRequest request) {
        Users actor = findUser(requireCurrentUserId());
        GroomingTicket ticket = findLockedTicket(ticketId);
        assertCanAccessTicket(ticket);
        
        boolean isStaff = SecurityContextService.hasAnyRole("ADMIN", "STAFF");
        ticket.cancel(request.reason(), OffsetDateTime.now(), actor.getId(), isStaff);
        
        appointmentRepository.save(ticket.getAppointment());
        serviceOrderRepository.save(ticket.getAppointment().getServiceOrder());
        return toTicketResponse(groomingTicketRepository.save(ticket));
    }
public List<GroomingServiceResponse> listGroomingServicesForAdmin() {
        return serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(ServiceCategory.GROOMING).stream()
                .map(groomingMapper::toServiceResponse)
                .toList();
    }
@Transactional
    public GroomingServiceResponse createGroomingService(GroomingServiceRequest request) {
        validateGroomingServiceRequest(request);
        if (serviceCatalogRepository.existsByServiceCode(request.serviceCode())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_007_SERVICE_CODE_EXISTS);
        }
        ServiceCatalog service = new ServiceCatalog();
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setCategoryCode(ServiceCategory.GROOMING);
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        service.setIsActive(true);
        return groomingMapper.toServiceResponse(serviceCatalogRepository.save(service));
    }
@Transactional
    public GroomingServiceResponse updateGroomingService(UUID id, GroomingServiceRequest request) {
        validateGroomingServiceRequest(request);
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .filter(c -> c.getCategoryCode() == ServiceCategory.GROOMING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND));
        if (serviceCatalogRepository.existsByServiceCodeAndIdNot(request.serviceCode(), id)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_007_SERVICE_CODE_EXISTS);
        }
        service.setServiceCode(request.serviceCode());
        service.setName(request.name());
        service.setDescription(request.description());
        service.setBasePriceVnd(request.basePriceVnd());
        service.setDurationMinutes(request.durationMinutes());
        return groomingMapper.toServiceResponse(serviceCatalogRepository.save(service));
    }
@Transactional
    public void deactivateGroomingService(UUID id) {
        ServiceCatalog service = serviceCatalogRepository.findById(id)
                .filter(c -> c.getCategoryCode() == ServiceCategory.GROOMING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_002_SERVICE_NOT_FOUND));
        service.setIsActive(false);
        serviceCatalogRepository.save(service);
    }
public List<GroomingStationResponse> listStationsForAdmin() {
        return groomingStationRepository.findAll().stream()
                .map(groomingMapper::toStationResponse)
                .toList();
    }
@Transactional
    public GroomingStationResponse createStation(GroomingStationRequest request) {
        if (groomingStationRepository.existsByStationCode(request.stationCode())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_008_STATION_CODE_EXISTS);
        }
        GroomingStation station = new GroomingStation();
        station.setStationCode(request.stationCode());
        station.setName(request.name());
        station.setIsActive(request.isActive());
        return groomingMapper.toStationResponse(groomingStationRepository.save(station));
    }
@Transactional
    public GroomingStationResponse updateStation(UUID id, GroomingStationRequest request) {
        GroomingStation station = groomingStationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        if (groomingStationRepository.existsByStationCodeAndIdNot(request.stationCode(), id)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_008_STATION_CODE_EXISTS);
        }
        station.setStationCode(request.stationCode());
        station.setName(request.name());
        station.setIsActive(request.isActive());
        return groomingMapper.toStationResponse(groomingStationRepository.save(station));
    }
@Transactional
    public void deactivateStation(UUID id) {
        GroomingStation station = groomingStationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_005_STATION_NOT_FOUND));
        station.setIsActive(false);
        groomingStationRepository.save(station);
    }

    private GroomingTicketResponse toTicketResponse(GroomingTicket ticket) {
        Invoice invoice = invoiceRepository.findByServiceOrderId(ticket.getAppointment().getServiceOrder().getId()).orElse(null);
        return groomingMapper.toTicketResponse(ticket, invoice);
    }

    private GroomingTicket findTicket(UUID ticketId) {
        return groomingTicketRepository.findWithDetailsById(ticketId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_GROOMING_001_TICKET_NOT_FOUND));
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
        UUID currentUserId = SecurityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private void assertCanAccessTicket(GroomingTicket ticket) {
        if (SecurityContextService.hasAnyRole("ADMIN", "STAFF")) {
            return;
        }
        UUID currentUserId = requireCurrentUserId();
        if (!ticket.getAppointment().getServiceOrder().getOwner().getId().equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }

    private void requireStatus(GroomingTicket ticket, GroomingStatus expectedStatus) {
        // Method retained for potential legacy usages, though internal transitions handle status.
        if (ticket.getStatusCode() != expectedStatus) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_004_INVALID_STATUS_TRANSITION);
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

    private void validateGroomingServiceRequest(GroomingServiceRequest request) {
        if (request.basePriceVnd() == null || request.basePriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (request.durationMinutes() == null || request.durationMinutes() < 1) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void ensureStationAvailable(UUID stationId, GroomingTicket ticket, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (groomingTicketRepository.existsStationConflict(stationId, STATION_BLOCKING_STATUSES, startAt, endAt, ticket.getId())) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_006_STATION_UNAVAILABLE);
        }
    }

    private void ensureOwnerBookingNotDuplicated(UUID ownerId, UUID petId, UUID serviceId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (groomingTicketRepository.existsOwnerBookingConflict(
                ownerId,
                petId,
                serviceId,
                OWNER_DUPLICATE_BLOCKING_STATUSES,
                startAt,
                endAt)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_009_DUPLICATE_BOOKING);
        }
    }

    private String generateCode(String prefix) {
        return prefix + "-" + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}





