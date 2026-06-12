package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.request.CreateMedicalAppointmentRequest;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.appointment.entity.ServiceCatalog;
import com.astral.express.pccms.appointment.entity.ServiceCategory;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.appointment.repository.ServiceCatalogRepository;
import com.astral.express.pccms.appointment.repository.ServiceOrderRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.pet.entity.Pets;
import com.astral.express.pccms.pet.repository.PetRepository;
import com.astral.express.pccms.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CreateMedicalAppointmentUseCase {

    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final String MEDICAL_SERVICE_CODE = "MED-GENERAL";

    private final PetRepository petRepository;
    private final ServiceCatalogRepository serviceCatalogRepository;
    private final ServiceOrderRepository serviceOrderRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentAvailabilityService availabilityService;
    private final VetAvailabilityChecker vetAvailabilityChecker;
    private final RoomAvailabilityChecker roomAvailabilityChecker;
    private final ServiceOrderFactory serviceOrderFactory;
    private final PetValidationService petValidationService;

    public Appointment createMedicalAppointment(CreateMedicalAppointmentRequest request, UUID ownerId) {
        Pets pet = petValidationService.findPetOwnedBy(request.petId(), ownerId);
        validateFutureDate(request.appointmentDate());
        int slotMinutes = resolveSlotMinutes();

        OffsetDateTime startAt = ClinicDateTime.toOffsetDateTime(request.appointmentDate(), request.slotStart());
        OffsetDateTime endAt = startAt.plusMinutes(slotMinutes);

        validateSlotNotPast(startAt);
        if (!availabilityService.isSlotAvailable(startAt, endAt, request.requestedVetId())) {
            throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
        }

        Users assignedVet = vetAvailabilityChecker.requireVetAvailable(request.appointmentDate(), request.slotStart(), request.requestedVetId(), startAt, endAt);
        ExamRoom examRoom = roomAvailabilityChecker.requireRoomAvailable(startAt, endAt);

        ServiceCatalog service = serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .orElseGet(() -> serviceCatalogRepository.findFirstByCategoryCodeAndIsActiveTrue(ServiceCategory.MEDICAL)
                        .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_006_SERVICE_NOT_FOUND)));

        ServiceOrder order = serviceOrderFactory.createServiceOrder(pet, service, ownerId, startAt, endAt, ServiceCategory.MEDICAL);
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

        return appointmentRepository.save(appointment);
    }

    private void validateFutureDate(LocalDate date) {
        if (date.isBefore(ClinicDateTime.today())) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
    }

    private void validateSlotNotPast(OffsetDateTime startAt) {
        if (startAt.isBefore(ClinicDateTime.now())) {
            throw new BusinessException(ErrorCode.ERR_APT_002_PAST_DATETIME);
        }
    }

    private int resolveSlotMinutes() {
        return serviceCatalogRepository.findByServiceCodeAndIsActiveTrue(MEDICAL_SERVICE_CODE)
                .map(ServiceCatalog::getDurationMinutes)
                .filter(m -> m != null && m > 0)
                .orElse(DEFAULT_SLOT_MINUTES);
    }
}
