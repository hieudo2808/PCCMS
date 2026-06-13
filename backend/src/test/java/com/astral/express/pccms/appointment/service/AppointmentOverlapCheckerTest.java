package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.repository.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppointmentOverlapCheckerTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentOverlapChecker checker;

    @Test
    void hasVetOverlap_shouldReturnTrue_whenCountGreaterThanZero() {
        UUID vetId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(appointmentRepository.countOverlappingForStaff(vetId, startAt, endAt)).willReturn(1L);

        boolean result = checker.hasVetOverlap(vetId, startAt, endAt);

        assertThat(result).isTrue();
    }

    @Test
    void hasVetOverlap_shouldReturnFalse_whenCountIsZero() {
        UUID vetId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(appointmentRepository.countOverlappingForStaff(vetId, startAt, endAt)).willReturn(0L);

        boolean result = checker.hasVetOverlap(vetId, startAt, endAt);

        assertThat(result).isFalse();
    }

    @Test
    void hasRoomOverlap_shouldReturnTrue_whenCountGreaterThanZero() {
        UUID roomId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(appointmentRepository.countOverlappingInRoom(roomId, startAt, endAt)).willReturn(1L);

        boolean result = checker.hasRoomOverlap(roomId, startAt, endAt);

        assertThat(result).isTrue();
    }

    @Test
    void hasRoomOverlap_shouldReturnFalse_whenCountIsZero() {
        UUID roomId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(appointmentRepository.countOverlappingInRoom(roomId, startAt, endAt)).willReturn(0L);

        boolean result = checker.hasRoomOverlap(roomId, startAt, endAt);

        assertThat(result).isFalse();
    }

    @Test
    void countGroomingOverlap_shouldReturnCount() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(appointmentRepository.countOverlappingGrooming(startAt, endAt)).willReturn(5L);

        long result = checker.countGroomingOverlap(startAt, endAt);

        assertThat(result).isEqualTo(5L);
    }
}
