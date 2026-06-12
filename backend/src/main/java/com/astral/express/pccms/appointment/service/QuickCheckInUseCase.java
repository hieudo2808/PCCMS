package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.QuickCheckInRequest;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.entity.*;
import com.astral.express.pccms.appointment.repository.*;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class QuickCheckInUseCase {

    private static final LocalTime CLINIC_CLOSE = LocalTime.of(17, 0);
    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final String MEDICAL_SERVICE_CODE = "MED-GENERAL";

    private final UserRepository userRepository;
    private final PetValidationService petValidationService;
    private final AppointmentAvailabilityService availabilityService;
    private final VetAvailabilityChecker vetAvailabilityChecker;
    private final RoomAvailabilityChecker roomAvailabilityChecker;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderFactory serviceOrderFactory;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final ReceptionTicketService receptionService;

    public Result execute(QuickCheckInRequest request, UUID staffId) {
        if (request.phone() == null || request.phone().isBlank()) {
            throw new BusinessException(ErrorCode.ERR_APT_008_PHONE_REQUIRED);
        }

        Users owner = userRepository.findByNormalizedPhone(normalizePhone(request.phone()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));

        Pets pet = petValidationService.findPetOwnedBy(request.petId(), owner.getId());

        LocalDate today = ClinicDateTime.today();
        LocalTime nowTime = ClinicDateTime.nowTime();
        LocalTime slotStart = roundUpToSlot(nowTime, resolveSlotMinutes());
        if (slotStart.plusMinutes(resolveSlotMinutes()).isAfter(CLINIC_CLOSE)) {
            throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
        }

        int slotMinutes = resolveSlotMinutes();
        OffsetDateTime startAt = ClinicDateTime.toOffsetDateTime(today, slotStart);
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        if (!availabilityService.isSlotAvailable(startAt, endAt, request.assignedVetId())) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }
        Users assignedVet = vetAvailabilityChecker.requireVetAvailable(today, slotStart, request.assignedVetId(), startAt, endAt);
        ExamRoom examRoom = roomAvailabilityChecker.requireRoomAvailable(startAt, endAt);

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .orElseGet(() -> serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)));

        ServiceOrder order = serviceOrderFactory.createServiceOrder(pet, service, staffId, startAt, endAt, ServiceCategory.MEDICAL);
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

        int nextQueue = receptionService.receiveAppointment(appointment, staff, assignedVet);

        return new Result(appointment, nextQueue);
    }

    private int resolveSlotMinutes() {
        return serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .map(ServiceCatalog::getDurationMinutes)
                .filter(m -> m != null && m > 0)
                .orElse(DEFAULT_SLOT_MINUTES);
    }

    private LocalTime roundUpToSlot(LocalTime time, int slotMinutes) {
        int totalMinutes = time.getHour() * 60 + time.getMinute();
        int remainder = totalMinutes % slotMinutes;
        if (remainder == 0) {
            return time;
        }
        int nextSlotMinutes = totalMinutes + (slotMinutes - remainder);
        int hours = nextSlotMinutes / 60;
        int mins = nextSlotMinutes % 60;
        if (hours >= 24) {
            return LocalTime.MAX;
        }
        return LocalTime.of(hours, mins);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    public record Result(Appointment appointment, int queueNumber) {}
}
