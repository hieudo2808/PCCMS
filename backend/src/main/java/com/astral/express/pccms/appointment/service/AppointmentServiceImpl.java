package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateBoardingBookingRequest;
import com.astral.express.pccms.appointment.dto.request.CreateGroomingAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.dto.request.QuickCheckInRequest;
import com.astral.express.pccms.appointment.dto.request.UpdateGroomingStatusRequest;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.dto.response.AvailabilitySummaryResponse;
import com.astral.express.pccms.appointment.dto.response.BoardingBookingResponse;
import com.astral.express.pccms.appointment.dto.response.CustomerLookupResponse;
import com.astral.express.pccms.appointment.dto.response.GroomingBoardCardResponse;
import com.astral.express.pccms.appointment.dto.response.QueueEntryResponse;
import com.astral.express.pccms.appointment.dto.response.RoomTypeOptionResponse;
import com.astral.express.pccms.appointment.dto.response.ServiceCatalogOptionResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.dto.response.VetOptionResponse;
import com.astral.express.pccms.appointment.entity.*;
import com.astral.express.pccms.appointment.repository.*;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {

    private static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalTime CLINIC_OPEN = LocalTime.of(8, 0);
    private static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);
    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final String MEDICAL_SERVICE_CODE = "MED-GENERAL";
    private static final String VET_ROLE = "VETERINARIAN";

    private final AppointmentRepository appointmentRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ReceptionTicketRepository receptionTicketRepository;
    private final ExamRoomRepository examRoomRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final GroomingStationRepository groomingStationRepository;
    private final GroomingTicketRepository groomingTicketRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final BoardingBookingRepository boardingBookingRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AppointmentResponse createMedicalAppointment(CreateMedicalAppointmentRequest request, UUID ownerId) {
        Pets pet = findPetOwnedBy(request.petId(), ownerId);
        validateFutureDate(request.appointmentDate());
        int slotMinutes = resolveSlotMinutes();

        OffsetDateTime startAt = toOffsetDateTime(request.appointmentDate(), request.slotStart());
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        validateSlotNotPast(startAt);
        ensureSlotCapacityAvailable(startAt, endAt, request.requestedVetId());

        Users assignedVet = resolveVet(request.appointmentDate(), request.slotStart(), request.requestedVetId(), startAt, endAt);
        ExamRoom examRoom = resolveExamRoom(startAt, endAt);

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .orElseGet(() -> serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)));

        ServiceOrder order = buildServiceOrder(pet, service, ownerId, startAt, endAt, ServiceCategory.MEDICAL);
        serviceOrderRepository.save(order);

        Appointment appointment = new Appointment();
        appointment.setServiceOrder(order);
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        appointment.setScheduledStartAt(startAt);
        appointment.setScheduledEndAt(endAt);
        appointment.setRequestedStaff(request.requestedVetId() != null ? assignedVet : null);
        appointment.setAssignedStaff(assignedVet);
        appointment.setExamRoom(examRoom);
        appointment.setStatusCode(AppointmentStatus.PENDING);
        appointment.setSymptomText(request.symptomText());
        appointment.setOwnerNote(request.ownerNote());
        appointment.setCreatedBy(ownerId);

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AppointmentResponse> listOwnerAppointments(UUID ownerId, Pageable pageable) {
        Page<Appointment> page = appointmentRepository.findByOwnerId(ownerId, pageable);
        return PageResponse.of(page.map(a -> toResponse(a, findQueueNumber(a.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> listTodayAppointments(
            LocalDate date, AppointmentStatus status, String phone, String customerName) {
        LocalDate targetDate = date != null ? date : LocalDate.now(CLINIC_ZONE);
        OffsetDateTime dayStart = targetDate.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = targetDate.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();

        String phoneNeedle = phone != null && !phone.isBlank() ? normalizePhone(phone) : null;
        String nameNeedle = customerName != null && !customerName.isBlank()
                ? customerName.trim().toLowerCase()
                : null;

        return appointmentRepository.findAppointmentsForDay(dayStart, dayEnd).stream()
                .filter(a -> status == null || a.getStatusCode() == status)
                .filter(a -> phoneNeedle == null || matchesPhone(a.getServiceOrder().getOwner().getPhone(), phoneNeedle))
                .filter(a -> nameNeedle == null || containsIgnoreCase(a.getServiceOrder().getOwner().getFullName(), nameNeedle))
                .map(a -> toResponse(a, findQueueNumber(a.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(LocalDate date, UUID vetId) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (date.isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
        int slotMinutes = resolveSlotMinutes();
        List<TimeSlotResponse> slots = new ArrayList<>();

        LocalTime cursor = CLINIC_OPEN;
        while (cursor.plusMinutes(slotMinutes).compareTo(CLINIC_CLOSE) <= 0) {
            LocalTime slotEnd = cursor.plusMinutes(slotMinutes);
            OffsetDateTime startAt = toOffsetDateTime(date, cursor);
            OffsetDateTime endAt = toOffsetDateTime(date, slotEnd);

            boolean available = !startAt.isBefore(OffsetDateTime.now(CLINIC_ZONE))
                    && isSlotAvailable(startAt, endAt, vetId);
            slots.add(new TimeSlotResponse(cursor, slotEnd, formatSlotLabel(cursor, slotEnd), available));
            cursor = slotEnd;
        }
        return slots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VetOptionResponse> listAvailableVets(LocalDate date, LocalTime slotStart) {
        List<Users> vets = userRepository.findActiveByRoleCode(VET_ROLE);
        if (vets.isEmpty()) {
            return List.of();
        }

        int slotMinutes = resolveSlotMinutes();
        OffsetDateTime startAt = toOffsetDateTime(date, slotStart);
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        return vets.stream()
                .map(vet -> {
                    boolean onDuty = isVetOnDuty(date, slotStart, vet.getId());
                    boolean free = onDuty && appointmentRepository.countOverlappingForStaff(vet.getId(), startAt, endAt) == 0;
                    return new VetOptionResponse(vet.getId(), formatVetName(vet.getFullName()), free);
                })
                .sorted(Comparator.comparing(VetOptionResponse::available).reversed()
                        .thenComparing(VetOptionResponse::fullName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VetOptionResponse> listVetsOnDuty(LocalDate date) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        List<Users> vets = userRepository.findActiveByRoleCode(VET_ROLE);
        if (vets.isEmpty()) {
            return List.of();
        }
        List<UUID> scheduledVetIds = workScheduleRepository.findVetIdsOnDutyForDate(date);
        boolean useAllVets = scheduledVetIds.isEmpty();

        return vets.stream()
                .map(vet -> {
                    boolean onDuty = useAllVets || scheduledVetIds.contains(vet.getId());
                    return new VetOptionResponse(vet.getId(), formatVetName(vet.getFullName()), onDuty);
                })
                .sorted(Comparator.comparing(VetOptionResponse::available).reversed()
                        .thenComparing(VetOptionResponse::fullName))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AvailabilitySummaryResponse getAvailabilitySummary(LocalDate date, LocalTime slotStart) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        List<TimeSlotResponse> slots = getAvailableSlots(date, null);
        int availableSlots = (int) slots.stream().filter(TimeSlotResponse::available).count();
        int totalRooms = examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().size();
        int vetsOnDuty = (int) listVetsOnDuty(date).stream().filter(VetOptionResponse::available).count();

        Integer freeRoomsForSlot = null;
        Integer freeVetsForSlot = null;
        if (slotStart != null) {
            int slotMinutes = resolveSlotMinutes();
            OffsetDateTime startAt = toOffsetDateTime(date, slotStart);
            OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);
            freeRoomsForSlot = (int) examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().stream()
                    .filter(room -> appointmentRepository.countOverlappingInRoom(room.getId(), startAt, endAt) == 0)
                    .count();
            freeVetsForSlot = (int) listAvailableVets(date, slotStart).stream()
                    .filter(VetOptionResponse::available)
                    .count();
        }

        return new AvailabilitySummaryResponse(
                totalRooms, vetsOnDuty, slots.size(), availableSlots, freeRoomsForSlot, freeVetsForSlot);
    }

    @Override
    @Transactional
    public AppointmentResponse checkIn(UUID appointmentId, UUID staffId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (appointment.getStatusCode() == AppointmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ERR_APT_003_ALREADY_CANCELLED);
        }
        if (appointment.getStatusCode() == AppointmentStatus.CHECKED_IN
                || appointment.getStatusCode() == AppointmentStatus.IN_PROGRESS
                || appointment.getStatusCode() == AppointmentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.ERR_APT_004_ALREADY_CHECKED_IN);
        }

        Users staff = userRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        Users vet = appointment.getAssignedStaff();
        if (vet == null) {
            throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
        }

        appointment.setStatusCode(AppointmentStatus.CHECKED_IN);
        ServiceOrder order = appointment.getServiceOrder();
        order.setStatusCode(ServiceOrderStatus.CONFIRMED);
        order.setUpdatedBy(staffId);

        LocalDate today = LocalDate.now(CLINIC_ZONE);
        OffsetDateTime dayStart = today.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        int nextQueue = receptionTicketRepository.findMaxQueueNumberForVet(vet.getId(), dayStart, dayEnd) + 1;

        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setAppointment(appointment);
        ticket.setCheckedInBy(staff);
        ticket.setCheckedInAt(OffsetDateTime.now(CLINIC_ZONE));
        ticket.setQueueNumber(nextQueue);
        ticket.setAssignedVet(vet);
        receptionTicketRepository.save(ticket);

        return toResponse(appointment, nextQueue);
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(UUID appointmentId, UUID actorId, boolean isStaff) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);

        if (!isStaff && !appointment.getServiceOrder().getOwner().getId().equals(actorId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (appointment.getStatusCode() == AppointmentStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ERR_APT_003_ALREADY_CANCELLED);
        }
        if (!isStaff && appointment.getStatusCode() != AppointmentStatus.PENDING) {
            throw new BusinessException(ErrorCode.ERR_APT_007_CANNOT_CANCEL);
        }
        if (isStaff && (appointment.getStatusCode() == AppointmentStatus.IN_PROGRESS
                || appointment.getStatusCode() == AppointmentStatus.COMPLETED)) {
            throw new BusinessException(ErrorCode.ERR_APT_007_CANNOT_CANCEL);
        }

        appointment.setStatusCode(AppointmentStatus.CANCELLED);
        ServiceOrder order = appointment.getServiceOrder();
        order.setStatusCode(ServiceOrderStatus.CANCELLED);
        order.setCancelledAt(OffsetDateTime.now(CLINIC_ZONE));
        order.setUpdatedBy(actorId);

        return toResponse(appointment, findQueueNumber(appointment.getId()));
    }

    @Override
    @Transactional
    public AppointmentResponse quickCheckIn(QuickCheckInRequest request, UUID staffId) {
        if (request.phone() == null || request.phone().isBlank()) {
            throw new BusinessException(ErrorCode.ERR_APT_008_PHONE_REQUIRED);
        }

        Users owner = userRepository.findByNormalizedPhone(normalizePhone(request.phone()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        Pets pet = findPetOwnedBy(request.petId(), owner.getId());

        LocalDate today = LocalDate.now(CLINIC_ZONE);
        LocalTime nowTime = LocalTime.now(CLINIC_ZONE);
        LocalTime slotStart = roundUpToSlot(nowTime, resolveSlotMinutes());
        if (slotStart.plusMinutes(resolveSlotMinutes()).isAfter(CLINIC_CLOSE)) {
            throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
        }

        int slotMinutes = resolveSlotMinutes();
        OffsetDateTime startAt = toOffsetDateTime(today, slotStart);
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        ensureSlotCapacityAvailable(startAt, endAt, request.assignedVetId());
        Users assignedVet = resolveVet(today, slotStart, request.assignedVetId(), startAt, endAt);
        ExamRoom examRoom = resolveExamRoom(startAt, endAt);

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .orElseGet(() -> serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)));

        ServiceOrder order = buildServiceOrder(pet, service, staffId, startAt, endAt, ServiceCategory.MEDICAL);
        serviceOrderRepository.save(order);

        Appointment appointment = new Appointment();
        appointment.setServiceOrder(order);
        appointment.setAppointmentType(AppointmentType.MEDICAL);
        appointment.setScheduledStartAt(startAt);
        appointment.setScheduledEndAt(endAt);
        appointment.setAssignedStaff(assignedVet);
        appointment.setExamRoom(examRoom);
        appointment.setStatusCode(AppointmentStatus.CHECKED_IN);
        appointment.setSymptomText(request.symptomText());
        appointment.setCreatedBy(staffId);
        appointmentRepository.save(appointment);

        order.setStatusCode(ServiceOrderStatus.CONFIRMED);

        Users staff = userRepository.findById(staffId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        OffsetDateTime dayStart = today.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = today.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        int nextQueue = receptionTicketRepository.findMaxQueueNumberForVet(assignedVet.getId(), dayStart, dayEnd) + 1;

        ReceptionTicket ticket = new ReceptionTicket();
        ticket.setAppointment(appointment);
        ticket.setCheckedInBy(staff);
        ticket.setCheckedInAt(OffsetDateTime.now(CLINIC_ZONE));
        ticket.setQueueNumber(nextQueue);
        ticket.setAssignedVet(assignedVet);
        receptionTicketRepository.save(ticket);

        return toResponse(appointment, nextQueue);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerLookupResponse lookupCustomerByPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new BusinessException(ErrorCode.ERR_APT_008_PHONE_REQUIRED);
        }
        Users owner = userRepository.findByNormalizedPhone(normalizePhone(phone))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        List<CustomerLookupResponse.PetSummary> pets = petRepository
                .findByOwnerIdAndIsActive(owner.getId(), true, PageRequest.of(0, 100))
                .getContent()
                .stream()
                .map(p -> new CustomerLookupResponse.PetSummary(p.getId(), p.getName()))
                .toList();

        return new CustomerLookupResponse(owner.getId(), owner.getFullName(), owner.getPhone(), pets);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueEntryResponse> getVetQueue(UUID vetId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(CLINIC_ZONE);
        OffsetDateTime dayStart = targetDate.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = targetDate.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();

        return receptionTicketRepository
                .findVetQueueTickets(vetId, dayStart, dayEnd, AppointmentStatus.CHECKED_IN).stream()
                .map(rt -> {
                    Appointment a = rt.getAppointment();
                    return new QueueEntryResponse(
                            rt.getQueueNumber(),
                            a.getId(),
                            a.getServiceOrder().getPet().getId(),
                            a.getServiceOrder().getPet().getName(),
                            a.getServiceOrder().getOwner().getFullName(),
                            rt.getCheckedInAt(),
                            a.getSymptomText()
                    );
                })
                .toList();
    }

    private Appointment findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));
    }

    private Pets findPetOwnedBy(UUID petId, UUID ownerId) {
        Pets pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND));
        if (!pet.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
        if (!Boolean.TRUE.equals(pet.getIsActive())) {
            throw new BusinessException(ErrorCode.ERR_PET_001_NOT_FOUND);
        }
        return pet;
    }

    private void validateFutureDate(LocalDate date) {
        if (date.isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
    }

    private void validateSlotNotPast(OffsetDateTime startAt) {
        if (startAt.isBefore(OffsetDateTime.now(CLINIC_ZONE))) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
    }

    private void ensureSlotCapacityAvailable(OffsetDateTime startAt, OffsetDateTime endAt, UUID requestedVetId) {
        if (!isSlotAvailable(startAt, endAt, requestedVetId)) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
    }

    private boolean isSlotAvailable(OffsetDateTime startAt, OffsetDateTime endAt, UUID requestedVetId) {
        List<ExamRoom> rooms = examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc();
        if (rooms.isEmpty()) {
            return false;
        }

        long freeRooms = rooms.stream()
                .filter(room -> appointmentRepository.countOverlappingInRoom(room.getId(), startAt, endAt) == 0)
                .count();
        if (freeRooms == 0) {
            return false;
        }

        if (requestedVetId != null) {
            return isVetOnDuty(startAt.toLocalDate(), startAt.toLocalTime(), requestedVetId)
                    && appointmentRepository.countOverlappingForStaff(requestedVetId, startAt, endAt) == 0;
        }

        List<UUID> vetCandidates = resolveVetCandidates(
                startAt.toLocalDate(), startAt.toLocalTime(), startAt, endAt);
        if (vetCandidates.isEmpty()) {
            return freeRooms > 0;
        }

        return vetCandidates.stream()
                .anyMatch(vetId -> appointmentRepository.countOverlappingForStaff(vetId, startAt, endAt) == 0);
    }

    private Users resolveVet(LocalDate date, LocalTime slotStart, UUID requestedVetId,
                             OffsetDateTime startAt, OffsetDateTime endAt) {
        if (requestedVetId != null) {
            Users vet = userRepository.findById(requestedVetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
            if (!VET_ROLE.equals(vet.getRole().getCode())) {
                throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
            }
            if (!isVetOnDuty(date, slotStart, requestedVetId)
                    || appointmentRepository.countOverlappingForStaff(requestedVetId, startAt, endAt) > 0) {
                throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
            }
            return vet;
        }

        List<UUID> candidates = resolveVetCandidates(date, slotStart, startAt, endAt);
        return candidates.stream()
                .map(userRepository::findById)
                .flatMap(java.util.Optional::stream)
                .filter(v -> appointmentRepository.countOverlappingForStaff(v.getId(), startAt, endAt) == 0)
                .min(Comparator.comparing(v -> appointmentRepository.countOverlappingForStaff(v.getId(), startAt, endAt)))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE));
    }

    private List<UUID> resolveVetCandidates(LocalDate date, LocalTime slotStart,
                                            OffsetDateTime startAt, OffsetDateTime endAt) {
        List<UUID> scheduledVetIds = workScheduleRepository.findAvailableVetIds(date, slotStart);
        if (!scheduledVetIds.isEmpty()) {
            return scheduledVetIds;
        }
        return userRepository.findActiveByRoleCode(VET_ROLE).stream().map(Users::getId).toList();
    }

    private boolean isVetOnDuty(LocalDate date, LocalTime slotStart, UUID vetId) {
        List<UUID> scheduledVetIds = workScheduleRepository.findAvailableVetIds(date, slotStart);
        if (scheduledVetIds.isEmpty()) {
            return userRepository.findActiveByRoleCode(VET_ROLE).stream()
                    .anyMatch(v -> v.getId().equals(vetId));
        }
        return scheduledVetIds.contains(vetId);
    }

    private ExamRoom resolveExamRoom(OffsetDateTime startAt, OffsetDateTime endAt) {
        return examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc().stream()
                .filter(room -> appointmentRepository.countOverlappingInRoom(room.getId(), startAt, endAt) == 0)
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL));
    }

    private ServiceOrder buildServiceOrder(Pets pet, ServiceCatalog service, UUID createdBy,
                                           OffsetDateTime startAt, OffsetDateTime endAt,
                                           ServiceCategory category) {
        ServiceOrder order = new ServiceOrder();
        order.setOrderCode(generateAppointmentCode());
        order.setOwner(pet.getOwner());
        order.setPet(pet);
        order.setService(service);
        order.setCategoryCode(category);
        order.setStatusCode(ServiceOrderStatus.REQUESTED);
        order.setRequestedAt(OffsetDateTime.now(CLINIC_ZONE));
        order.setPlannedStartAt(startAt);
        order.setPlannedEndAt(endAt);
        order.setBaseAmountVnd(service.getBasePriceVnd());
        order.setCreatedBy(createdBy);
        return order;
    }

    private String generateAppointmentCode() {
        long seq = serviceOrderRepository.maxAppointmentOrderSequence() + 1;
        return String.format("AP%04d", seq);
    }

    private int resolveSlotMinutes() {
        return serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .map(ServiceCatalog::getDurationMinutes)
                .filter(m -> m != null && m > 0)
                .orElse(DEFAULT_SLOT_MINUTES);
    }

    private OffsetDateTime toOffsetDateTime(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(CLINIC_ZONE).toOffsetDateTime();
    }

    private LocalTime roundUpToSlot(LocalTime time, int slotMinutes) {
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int remainder = totalMinutes % slotMinutes;
        int rounded = remainder == 0 ? totalMinutes : totalMinutes + (slotMinutes - remainder);
        if (rounded < CLINIC_OPEN.getHour() * 60 + CLINIC_OPEN.getMinute()) {
            return CLINIC_OPEN;
        }
        return LocalTime.of(rounded / 60, rounded % 60);
    }

    private String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("[\\s.\\-]", "");
    }

    private boolean matchesPhone(String ownerPhone, String needle) {
        return normalizePhone(ownerPhone).contains(needle);
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private String formatSlotLabel(LocalTime start, LocalTime end) {
        return String.format("%02d:%02d - %02d:%02d", start.getHour(), start.getMinute(), end.getHour(), end.getMinute());
    }

    private String formatVetName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Bác sĩ";
        }
        return fullName.startsWith("BS") ? fullName : "BS. " + fullName;
    }

    private Integer findQueueNumber(UUID appointmentId) {
        return receptionTicketRepository.findByAppointmentId(appointmentId)
                .map(ReceptionTicket::getQueueNumber)
                .orElse(null);
    }

    private AppointmentResponse toResponse(Appointment appointment, Integer queueNumber) {
        ServiceOrder order = appointment.getServiceOrder();
        Users owner = order.getOwner();
        Pets pet = order.getPet();
        Users vet = appointment.getAssignedStaff();

        return new AppointmentResponse(
                appointment.getId(),
                order.getOrderCode(),
                appointment.getAppointmentType(),
                order.getService().getName(),
                appointment.getScheduledStartAt(),
                appointment.getScheduledEndAt(),
                owner.getFullName(),
                owner.getPhone(),
                pet.getId(),
                pet.getName(),
                vet != null ? vet.getId() : null,
                vet != null ? formatVetName(vet.getFullName()) : null,
                appointment.getStatusCode(),
                toStatusLabel(appointment.getStatusCode()),
                appointment.getSymptomText(),
                appointment.getOwnerNote(),
                queueNumber
        );
    }

    @Override
    @Transactional
    public AppointmentResponse createGroomingAppointment(CreateGroomingAppointmentRequest request, UUID ownerId) {
        Pets pet = findPetOwnedBy(request.petId(), ownerId);
        validateFutureDate(request.appointmentDate());

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(request.serviceCode())
                .filter(s -> s.getCategoryCode() == ServiceCategory.GROOMING)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND));

        int durationMinutes = service.getDurationMinutes() != null && service.getDurationMinutes() > 0
                ? service.getDurationMinutes() : 60;

        OffsetDateTime startAt = toOffsetDateTime(request.appointmentDate(), request.slotStart());
        OffsetDateTime endAt = startAt.plusMinutes(durationMinutes);

        validateSlotNotPast(startAt);
        ensureGroomingSlotAvailable(startAt, endAt);

        ServiceOrder order = buildServiceOrder(pet, service, ownerId, startAt, endAt, ServiceCategory.GROOMING);
        serviceOrderRepository.save(order);

        Appointment appointment = new Appointment();
        appointment.setServiceOrder(order);
        appointment.setAppointmentType(AppointmentType.GROOMING);
        appointment.setScheduledStartAt(startAt);
        appointment.setScheduledEndAt(endAt);
        appointment.setStatusCode(AppointmentStatus.PENDING);
        appointment.setOwnerNote(request.ownerNote());
        appointment.setCreatedBy(ownerId);

        Appointment saved = appointmentRepository.save(appointment);

        GroomingTicket ticket = new GroomingTicket();
        ticket.setAppointment(saved);
        ticket.setStatusCode(GroomingStatus.PENDING);
        ticket.setOwnerNote(request.ownerNote());
        groomingTicketRepository.save(ticket);

        return toResponse(saved, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroomingBoardCardResponse> listGroomingBoard(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(CLINIC_ZONE);
        OffsetDateTime dayStart = targetDate.atStartOfDay(CLINIC_ZONE).toOffsetDateTime();
        OffsetDateTime dayEnd = targetDate.plusDays(1).atStartOfDay(CLINIC_ZONE).toOffsetDateTime();

        return groomingTicketRepository.findBoardForDate(dayStart, dayEnd, GroomingStatus.CANCELLED).stream()
                .map(this::toGroomingBoardCard)
                .toList();
    }

    @Override
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
        OffsetDateTime now = OffsetDateTime.now(CLINIC_ZONE);
        if (newStatus == GroomingStatus.IN_SERVICE && ticket.getStartedAt() == null) {
            ticket.setStartedAt(now);
            ticket.getAppointment().setStatusCode(AppointmentStatus.IN_PROGRESS);
        }
        if (newStatus == GroomingStatus.COMPLETED) {
            ticket.setCompletedAt(now);
            ticket.getAppointment().setStatusCode(AppointmentStatus.COMPLETED);
            ServiceOrder order = ticket.getAppointment().getServiceOrder();
            order.setStatusCode(ServiceOrderStatus.COMPLETED);
            order.setCompletedAt(now);
        }
        if (newStatus == GroomingStatus.CANCELLED) {
            ticket.getAppointment().setStatusCode(AppointmentStatus.CANCELLED);
            ServiceOrder order = ticket.getAppointment().getServiceOrder();
            order.setStatusCode(ServiceOrderStatus.CANCELLED);
            order.setCancelledAt(now);
        }

        return toGroomingBoardCard(ticket);
    }

    @Override
    @Transactional
    public BoardingBookingResponse createBoardingBooking(CreateBoardingBookingRequest request, UUID ownerId) {
        if (request.checkoutDate().isBefore(request.checkinDate())
                || request.checkoutDate().isEqual(request.checkinDate())) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (request.checkinDate().isBefore(LocalDate.now(CLINIC_ZONE))) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }

        Pets pet = findPetOwnedBy(request.petId(), ownerId);
        RoomType roomType = roomTypeRepository.findById(request.roomTypeId())
                .filter(RoomType::getIsActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_VALIDATION_FAILED));

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue("BRD-STAY")
                .orElseGet(() -> serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.BOARDING)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)));

        OffsetDateTime checkinAt = toOffsetDateTime(request.checkinDate(), LocalTime.of(14, 0));
        OffsetDateTime checkoutAt = toOffsetDateTime(request.checkoutDate(), LocalTime.of(11, 0));
        long days = ChronoUnit.DAYS.between(request.checkinDate(), request.checkoutDate());
        BigDecimal estimated = roomType.getBaseDailyPriceVnd()
                .multiply(BigDecimal.valueOf(Math.max(days, 1)));

        ServiceOrder order = buildServiceOrder(pet, service, ownerId, checkinAt, checkoutAt, ServiceCategory.BOARDING);
        order.setBaseAmountVnd(estimated);
        serviceOrderRepository.save(order);

        BoardingBooking booking = new BoardingBooking();
        booking.setBookingCode(generateBoardingCode());
        booking.setServiceOrder(order);
        booking.setOwner(pet.getOwner());
        booking.setPet(pet);
        booking.setRequestedRoomType(roomType);
        booking.setExpectedCheckinAt(checkinAt);
        booking.setExpectedCheckoutAt(checkoutAt);
        booking.setSpecialCareRequest(request.specialCareRequest());
        booking.setEstimatedPriceVnd(estimated);
        booking.setStatusCode(BoardingStatus.RESERVED);
        booking.setCreatedBy(ownerId);

        BoardingBooking saved = boardingBookingRepository.save(booking);
        return toBoardingResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardingBookingResponse> listOwnerBoardingBookings(UUID ownerId) {
        return boardingBookingRepository.findByOwnerId(ownerId).stream()
                .map(this::toBoardingResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomTypeOptionResponse> listActiveRoomTypes() {
        return roomTypeRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(rt -> new RoomTypeOptionResponse(rt.getId(), rt.getCode(), rt.getName(), rt.getBaseDailyPriceVnd()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceCatalogOptionResponse> listServicesByCategory(ServiceCategory category) {
        return serviceCatalogRepository.findByCategoryCodeAndIsActiveTrueOrderByNameAsc(category).stream()
                .map(s -> new ServiceCatalogOptionResponse(
                        s.getId(), s.getServiceCode(), s.getName(),
                        s.getCategoryCode(), s.getBasePriceVnd(), s.getDurationMinutes()))
                .toList();
    }

    private void ensureGroomingSlotAvailable(OffsetDateTime startAt, OffsetDateTime endAt) {
        int stations = groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc().size();
        if (stations == 0) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
        long booked = appointmentRepository.countOverlappingGrooming(startAt, endAt);
        if (booked >= stations) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
    }

    private boolean isValidGroomingTransition(GroomingStatus current, GroomingStatus next) {
        return switch (current) {
            case PENDING -> next == GroomingStatus.CONFIRMED || next == GroomingStatus.CANCELLED;
            case CONFIRMED -> next == GroomingStatus.IN_SERVICE || next == GroomingStatus.CANCELLED;
            case IN_SERVICE -> next == GroomingStatus.COMPLETED || next == GroomingStatus.CANCELLED;
            default -> false;
        };
    }

    private GroomingBoardCardResponse toGroomingBoardCard(GroomingTicket ticket) {
        Appointment appointment = ticket.getAppointment();
        ServiceOrder order = appointment.getServiceOrder();
        return new GroomingBoardCardResponse(
                ticket.getId(),
                appointment.getId(),
                order.getPet().getName(),
                order.getService().getName(),
                appointment.getScheduledStartAt(),
                ticket.getStatusCode(),
                toGroomingStatusLabel(ticket.getStatusCode()),
                ticket.getStation() != null ? ticket.getStation().getName() : null
        );
    }

    private BoardingBookingResponse toBoardingResponse(BoardingBooking booking) {
        return new BoardingBookingResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getPet().getId(),
                booking.getPet().getName(),
                booking.getRequestedRoomType().getName(),
                booking.getExpectedCheckinAt(),
                booking.getExpectedCheckoutAt(),
                booking.getEstimatedPriceVnd(),
                booking.getStatusCode(),
                toBoardingStatusLabel(booking.getStatusCode()),
                booking.getSpecialCareRequest()
        );
    }

    private String generateBoardingCode() {
        long seq = boardingBookingRepository.maxBookingSequence() + 1;
        return String.format("BR%04d", seq);
    }

    private String toGroomingStatusLabel(GroomingStatus status) {
        return switch (status) {
            case PENDING -> "Chờ làm";
            case CONFIRMED -> "Đã xác nhận";
            case IN_SERVICE -> "Đang dùng dịch vụ";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String toBoardingStatusLabel(BoardingStatus status) {
        return switch (status) {
            case RESERVED -> "Đã đặt phòng";
            case CHECKED_IN -> "Đã nhận phòng";
            case IN_STAY -> "Đang lưu trú";
            case CHECKED_OUT -> "Đã trả phòng";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String toStatusLabel(AppointmentStatus status) {
        return switch (status) {
            case PENDING -> "Chờ tiếp nhận";
            case CONFIRMED -> "Đã xác nhận";
            case CHECKED_IN -> "Đang chờ khám";
            case IN_PROGRESS -> "Đang khám";
            case COMPLETED -> "Hoàn thành";
            case CANCELLED -> "Đã hủy";
        };
    }
}
