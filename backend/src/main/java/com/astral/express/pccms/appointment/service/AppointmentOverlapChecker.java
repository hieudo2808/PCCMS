package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AppointmentOverlapChecker {
    private final AppointmentRepository appointmentRepository;

    public boolean hasVetOverlap(UUID vetId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return appointmentRepository.countOverlappingForStaff(vetId, startAt, endAt) > 0;
    }

    public boolean hasRoomOverlap(UUID roomId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return appointmentRepository.countOverlappingInRoom(roomId, startAt, endAt) > 0;
    }

    public long countGroomingOverlap(OffsetDateTime startAt, OffsetDateTime endAt) {
        return appointmentRepository.countOverlappingGrooming(startAt, endAt);
    }
}
