package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AvailabilitySummaryResponse;
import com.astral.express.pccms.appointment.dto.response.TimeSlotResponse;
import com.astral.express.pccms.appointment.dto.response.VetOptionResponse;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentAvailabilityUseCase {
    private static final LocalTime CLINIC_OPEN = LocalTime.of(8, 0);
    private static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);
    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final String MEDICAL_SERVICE_CODE = "MED-GENERAL";
    private static final String VET_ROLE = "VETERINARIAN";

    private final ServiceCatalogRepository serviceCatalogRepository;
    private final UserRepository userRepository;
    private final AppointmentResponseAssembler assembler;
    private final AppointmentAvailabilityService availabilityService;
    private final VetAvailabilityChecker vetAvailabilityChecker;
    private final RoomAvailabilityChecker roomAvailabilityChecker;

    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getAvailableSlots(LocalDate date, UUID vetId) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        if (date.isBefore(ClinicDateTime.today())) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
        int slotMinutes = resolveSlotMinutes();
        List<TimeSlotResponse> slots = new ArrayList<>();

        LocalTime cursor = CLINIC_OPEN;
        while (cursor.plusMinutes(slotMinutes).compareTo(CLINIC_CLOSE) <= 0) {
            LocalTime slotEnd = cursor.plusMinutes(slotMinutes);
            OffsetDateTime startAt = ClinicDateTime.toOffsetDateTime(date, cursor);
            OffsetDateTime endAt = ClinicDateTime.toOffsetDateTime(date, slotEnd);

            boolean available = !startAt.isBefore(ClinicDateTime.now())
                    && availabilityService.isSlotAvailable(startAt, endAt, vetId);
            slots.add(assembler.toTimeSlotResponse(cursor, slotEnd, available));
            cursor = slotEnd;
        }
        return slots;
    }

    @Transactional(readOnly = true)
    public List<VetOptionResponse> listAvailableVets(LocalDate date, LocalTime slotStart) {
        List<Users> vets = userRepository.findActiveByRoleCode(VET_ROLE);
        if (vets.isEmpty()) {
            return List.of();
        }

        int slotMinutes = resolveSlotMinutes();
        OffsetDateTime startAt = ClinicDateTime.toOffsetDateTime(date, slotStart);
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        return vets.stream()
                .map(vet -> {
                    boolean onDuty = vetAvailabilityChecker.isVetOnDuty(date, slotStart, vet.getId());
                    boolean free = onDuty && vetAvailabilityChecker.isVetFree(vet.getId(), startAt, endAt);
                    return assembler.toVetOptionResponse(vet, free);
                })
                .sorted(Comparator.comparing(VetOptionResponse::available).reversed()
                        .thenComparing(VetOptionResponse::fullName))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<VetOptionResponse> listVetsOnDuty(LocalDate date) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        List<Users> vets = userRepository.findActiveByRoleCode(VET_ROLE);
        if (vets.isEmpty()) {
            return List.of();
        }
        List<UUID> scheduledVetIds = vetAvailabilityChecker.findVetIdsOnDutyForDate(date);
        boolean useAllVets = scheduledVetIds.isEmpty();

        return vets.stream()
                .map(vet -> {
                    boolean onDuty = useAllVets || scheduledVetIds.contains(vet.getId());
                    return assembler.toVetOptionResponse(vet, onDuty);
                })
                .sorted(Comparator.comparing(VetOptionResponse::available).reversed()
                        .thenComparing(VetOptionResponse::fullName))
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailabilitySummaryResponse getAvailabilitySummary(LocalDate date, LocalTime slotStart) {
        if (date == null) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        List<TimeSlotResponse> slots = getAvailableSlots(date, null);
        int availableSlots = (int) slots.stream().filter(TimeSlotResponse::available).count();
        int totalRooms = roomAvailabilityChecker.getTotalActiveRooms();
        int vetsOnDuty = (int) listVetsOnDuty(date).stream().filter(VetOptionResponse::available).count();

        Integer freeRoomsForSlot = null;
        Integer freeVetsForSlot = null;
        if (slotStart != null) {
            int slotMinutes = resolveSlotMinutes();
            OffsetDateTime startAt = ClinicDateTime.toOffsetDateTime(date, slotStart);
            OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);
            freeRoomsForSlot = (int) roomAvailabilityChecker.countFreeRooms(startAt, endAt);
            freeVetsForSlot = (int) listAvailableVets(date, slotStart).stream()
                    .filter(VetOptionResponse::available)
                    .count();
        }

        return new AvailabilitySummaryResponse(
                totalRooms, vetsOnDuty, slots.size(), availableSlots, freeRoomsForSlot, freeVetsForSlot);
    }

    private int resolveSlotMinutes() {
        return serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .map(ServiceCatalog::getDurationMinutes)
                .filter(m -> m != null && m > 0)
                .orElse(DEFAULT_SLOT_MINUTES);
    }
}
