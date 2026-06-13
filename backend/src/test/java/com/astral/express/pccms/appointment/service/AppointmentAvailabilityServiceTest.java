package com.astral.express.pccms.appointment.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AppointmentAvailabilityServiceTest {

    @Mock
    private VetAvailabilityChecker vetAvailabilityChecker;

    @Mock
    private RoomAvailabilityChecker roomAvailabilityChecker;

    @Mock
    private AppointmentOverlapChecker overlapChecker;

    @InjectMocks
    private AppointmentAvailabilityService service;

    @Test
    void isSlotAvailable_shouldReturnFalse_whenNoRoomsAvailable() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(0L);

        boolean result = service.isSlotAvailable(start, end, null);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_shouldReturnTrue_whenRequestedVetAvailable() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);
        UUID vetId = UUID.randomUUID();

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.isVetOnDuty(any(), any(), eq(vetId))).willReturn(true);
        given(overlapChecker.hasVetOverlap(vetId, start, end)).willReturn(false);

        boolean result = service.isSlotAvailable(start, end, vetId);

        assertThat(result).isTrue();
    }

    @Test
    void isSlotAvailable_shouldReturnFalse_whenRequestedVetNotOnDuty() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);
        UUID vetId = UUID.randomUUID();

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.isVetOnDuty(any(), any(), eq(vetId))).willReturn(false);

        boolean result = service.isSlotAvailable(start, end, vetId);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_shouldReturnFalse_whenRequestedVetOverlaps() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);
        UUID vetId = UUID.randomUUID();

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.isVetOnDuty(any(), any(), eq(vetId))).willReturn(true);
        given(overlapChecker.hasVetOverlap(vetId, start, end)).willReturn(true);

        boolean result = service.isSlotAvailable(start, end, vetId);

        assertThat(result).isFalse();
    }

    @Test
    void isSlotAvailable_shouldReturnTrue_whenNoVetRequestedAndNoVetsOnDutyButRoomFree() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.resolveVetCandidates(any(), any())).willReturn(Collections.emptyList());

        boolean result = service.isSlotAvailable(start, end, null);

        assertThat(result).isTrue();
    }

    @Test
    void isSlotAvailable_shouldReturnTrue_whenAnyVetCandidateAvailable() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);
        UUID vetId1 = UUID.randomUUID();
        UUID vetId2 = UUID.randomUUID();

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.resolveVetCandidates(any(), any())).willReturn(List.of(vetId1, vetId2));
        given(overlapChecker.hasVetOverlap(vetId1, start, end)).willReturn(true);
        given(overlapChecker.hasVetOverlap(vetId2, start, end)).willReturn(false);

        boolean result = service.isSlotAvailable(start, end, null);

        assertThat(result).isTrue();
    }

    @Test
    void isSlotAvailable_shouldReturnFalse_whenAllVetCandidatesOverlap() {
        OffsetDateTime start = OffsetDateTime.now();
        OffsetDateTime end = start.plusMinutes(30);
        UUID vetId = UUID.randomUUID();

        given(roomAvailabilityChecker.countFreeRooms(start, end)).willReturn(1L);
        given(vetAvailabilityChecker.resolveVetCandidates(any(), any())).willReturn(List.of(vetId));
        given(overlapChecker.hasVetOverlap(vetId, start, end)).willReturn(true);

        boolean result = service.isSlotAvailable(start, end, null);

        assertThat(result).isFalse();
    }
}
