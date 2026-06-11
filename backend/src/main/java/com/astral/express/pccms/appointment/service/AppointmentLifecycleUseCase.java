package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.entity.Appointment;
import com.astral.express.pccms.appointment.entity.AppointmentStatus;
import com.astral.express.pccms.appointment.entity.AppointmentType;
import com.astral.express.pccms.appointment.entity.ServiceOrder;
import com.astral.express.pccms.appointment.entity.ServiceOrderStatus;
import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentLifecycleUseCase {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AppointmentResponseAssembler assembler;
    private final ReceptionTicketService receptionService;

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

        int nextQueue = receptionService.receiveAppointment(appointment, staff, vet);

        return assembler.toResponse(appointment, nextQueue);
    }

    @Transactional
    public AppointmentResponse startExam(UUID appointmentId, UUID vetId) {
        Appointment appointment = findAppointmentOrThrow(appointmentId);
        ensureMedicalAppointment(appointment);
        ensureAssignedVet(appointment, vetId);

        if (appointment.getStatusCode() == AppointmentStatus.COMPLETED
                || appointment.getStatusCode() == AppointmentStatus.IN_PROGRESS) {
            return assembler.toResponse(appointment, receptionService.getQueueNumberForAppointment(appointmentId));
        }
        if (appointment.getStatusCode() != AppointmentStatus.CHECKED_IN) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        OffsetDateTime now = ClinicDateTime.now();
        appointment.setStatusCode(AppointmentStatus.IN_PROGRESS);
        ServiceOrder order = appointment.getServiceOrder();
        order.setStatusCode(ServiceOrderStatus.IN_PROGRESS);
        if (order.getActualStartAt() == null) {
            order.setActualStartAt(now);
        }
        order.setUpdatedBy(vetId);

        return assembler.toResponse(appointment, receptionService.getQueueNumberForAppointment(appointmentId));
    }

    @Transactional
    public void completeMedicalAppointment(UUID appointmentId, UUID vetId) {
        if (appointmentId == null) {
            return;
        }

        Appointment appointment = findAppointmentOrThrow(appointmentId);
        ensureMedicalAppointment(appointment);
        ensureAssignedVet(appointment, vetId);

        if (appointment.getStatusCode() == AppointmentStatus.COMPLETED) {
            return;
        }
        if (appointment.getStatusCode() == AppointmentStatus.CANCELLED
                || appointment.getStatusCode() == AppointmentStatus.PENDING
                || appointment.getStatusCode() == AppointmentStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        OffsetDateTime now = ClinicDateTime.now();
        appointment.setStatusCode(AppointmentStatus.COMPLETED);
        ServiceOrder order = appointment.getServiceOrder();
        order.setStatusCode(ServiceOrderStatus.COMPLETED);
        if (order.getActualStartAt() == null) {
            order.setActualStartAt(now);
        }
        order.setCompletedAt(now);
        order.setUpdatedBy(vetId);
    }

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
        order.setCancelledAt(ClinicDateTime.now());
        order.setUpdatedBy(actorId);

        return assembler.toResponse(appointment, receptionService.getQueueNumberForAppointment(appointmentId));
    }

    private Appointment findAppointmentOrThrow(UUID appointmentId) {
        return appointmentRepository.findDetailById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_001_NOT_FOUND));
    }

    private void ensureMedicalAppointment(Appointment appointment) {
        if (appointment.getAppointmentType() != AppointmentType.MEDICAL) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void ensureAssignedVet(Appointment appointment, UUID vetId) {
        Users assignedVet = appointment.getAssignedStaff();
        if (assignedVet == null) {
            throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
        }
        if (vetId != null && !assignedVet.getId().equals(vetId)) {
            throw new BusinessException(ErrorCode.ERR_403_FORBIDDEN);
        }
    }
}
