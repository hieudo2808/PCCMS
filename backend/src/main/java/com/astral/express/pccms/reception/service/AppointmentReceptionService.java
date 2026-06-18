package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import com.astral.express.pccms.reception.repository.AppointmentReceptionCommandRepository;
import com.astral.express.pccms.reception.repository.AppointmentReceptionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentReceptionService {
    private final SecurityContextService securityContextService;
    private final AppointmentReceptionQueryRepository appointmentReceptionQueryRepository;
    private final AppointmentReceptionCommandRepository appointmentReceptionCommandRepository;

    @Transactional(readOnly = true)
    public List<AppointmentReceptionResponse> listAppointments(String keyword, String status) {
        return appointmentReceptionQueryRepository.listAppointments(keyword, status);
    }

    @Transactional
    public AppointmentReceptionResponse quickCreateAndReceive(QuickAppointmentRequest request) {
        ReceptionValidation.validateQuickAppointment(request.phone(), request.ownerName(), request.petName(), request.symptomText());
        UUID ownerId = appointmentReceptionCommandRepository.findOwnerIdByPhone(request.phone())
                .orElseGet(() -> appointmentReceptionCommandRepository.createWalkinOwner(
                        "walkin" + System.currentTimeMillis() + "@pccms.local",
                        request.phone(),
                        request.ownerName()));
        UUID petId = appointmentReceptionCommandRepository.findPetId(ownerId, request.petName())
                .orElseGet(() -> appointmentReceptionCommandRepository.createPet(ownerId, request.petName()));

        Timestamp start = ts(request.scheduledStartAt());
        if (start == null) {
            start = Timestamp.valueOf(LocalDateTime.now().plusHours(1));
        }
        Timestamp end = ts(request.scheduledEndAt());
        if (end == null) {
            end = Timestamp.from(start.toInstant().plusSeconds(1800));
        }

        UUID orderId = appointmentReceptionCommandRepository.createServiceOrder(
                code("SO-AP-"),
                ownerId,
                petId,
                start,
                end,
                securityContextService.getCurrentUserId(),
                valueOrDefault(request.serviceCode(), "MED-GENERAL"));
        UUID appointmentId = appointmentReceptionCommandRepository.createAppointment(
                orderId,
                start,
                end,
                request.doctorId(),
                request.symptomText(),
                request.ownerNote(),
                securityContextService.getCurrentUserId());

        return receive(appointmentId, new AppointmentReceiveRequest(request.doctorId(), "Tao nhanh tai quay"));
    }

    @Transactional
    public AppointmentReceptionResponse receive(UUID appointmentId, AppointmentReceiveRequest request) {
        String status = appointmentReceptionCommandRepository.findAppointmentStatus(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND));
        if ("CANCELLED".equals(status) || !List.of("PENDING", "CONFIRMED").contains(status)) {
            throw new BusinessException(ErrorCode.ERR_REC_002_APPOINTMENT_NOT_RECEIVABLE);
        }

        UUID doctorId = request == null ? null : request.doctorId();
        appointmentReceptionCommandRepository.receiveAppointment(appointmentId, doctorId);
        appointmentReceptionCommandRepository.createReceptionTicket(
                appointmentId,
                securityContextService.getCurrentUserId(),
                doctorId,
                request == null ? null : request.note());
        return getById(appointmentId);
    }

    @Transactional
    public AppointmentReceptionResponse cancel(UUID appointmentId, AppointmentCancelRequest request) {
        appointmentReceptionCommandRepository.cancelAppointment(appointmentId, request == null ? null : request.reason());
        return getById(appointmentId);
    }

    private AppointmentReceptionResponse getById(UUID appointmentId) {
        return appointmentReceptionQueryRepository.findById(appointmentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_REC_001_APPOINTMENT_NOT_FOUND));
    }

    private static String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Timestamp ts(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.length() == 10) {
            return Timestamp.valueOf(value + " 00:00:00");
        }
        try {
            return Timestamp.from(OffsetDateTime.parse(value).toInstant());
        } catch (Exception ignored) {
            return Timestamp.valueOf(LocalDateTime.parse(value));
        }
    }

    private String code(String prefix) {
        return prefix + System.currentTimeMillis();
    }
}
