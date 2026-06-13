package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.appointment.entity.ExamRoom;
import com.astral.express.pccms.appointment.repository.ExamRoomRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.grooming.entity.GroomingStation;
import com.astral.express.pccms.grooming.repository.GroomingStationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RoomAvailabilityCheckerTest {

    @Mock
    private ExamRoomRepository examRoomRepository;

    @Mock
    private GroomingStationRepository groomingStationRepository;

    @Mock
    private AppointmentOverlapChecker overlapChecker;

    @InjectMocks
    private RoomAvailabilityChecker checker;

    @Test
    void requireRoomAvailable_shouldReturnRoom_whenAvailable() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        ExamRoom room1 = new ExamRoom();
        room1.setId(UUID.randomUUID());
        ExamRoom room2 = new ExamRoom();
        room2.setId(UUID.randomUUID());

        given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(room1, room2));
        given(overlapChecker.hasRoomOverlap(room1.getId(), startAt, endAt)).willReturn(true);
        given(overlapChecker.hasRoomOverlap(room2.getId(), startAt, endAt)).willReturn(false);

        ExamRoom result = checker.requireRoomAvailable(startAt, endAt);

        assertThat(result.getId()).isEqualTo(room2.getId());
    }

    @Test
    void requireRoomAvailable_shouldThrowException_whenAllRoomsOverlap() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        ExamRoom room1 = new ExamRoom();
        room1.setId(UUID.randomUUID());

        given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(room1));
        given(overlapChecker.hasRoomOverlap(room1.getId(), startAt, endAt)).willReturn(true);

        assertThatThrownBy(() -> checker.requireRoomAvailable(startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
    }

    @Test
    void requireGroomingSlotAvailable_shouldThrowException_whenNoStations() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()).willReturn(List.of());

        assertThatThrownBy(() -> checker.requireGroomingSlotAvailable(startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
    }

    @Test
    void requireGroomingSlotAvailable_shouldThrowException_whenFullyBooked() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()).willReturn(List.of(new GroomingStation(), new GroomingStation())); // 2 stations
        given(overlapChecker.countGroomingOverlap(startAt, endAt)).willReturn(2L);

        assertThatThrownBy(() -> checker.requireGroomingSlotAvailable(startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_APT_009_SLOT_FULL);
    }

    @Test
    void requireGroomingSlotAvailable_shouldSuccess_whenSlotsAvailable() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        given(groomingStationRepository.findByIsActiveTrueOrderByStationCodeAsc()).willReturn(List.of(new GroomingStation(), new GroomingStation())); // 2 stations
        given(overlapChecker.countGroomingOverlap(startAt, endAt)).willReturn(1L);

        checker.requireGroomingSlotAvailable(startAt, endAt);
    }

    @Test
    void countFreeRooms_shouldReturnFreeCount() {
        OffsetDateTime startAt = OffsetDateTime.now();
        OffsetDateTime endAt = startAt.plusMinutes(30);

        ExamRoom room1 = new ExamRoom();
        room1.setId(UUID.randomUUID());
        ExamRoom room2 = new ExamRoom();
        room2.setId(UUID.randomUUID());

        given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(room1, room2));
        given(overlapChecker.hasRoomOverlap(room1.getId(), startAt, endAt)).willReturn(true);
        given(overlapChecker.hasRoomOverlap(room2.getId(), startAt, endAt)).willReturn(false);

        long result = checker.countFreeRooms(startAt, endAt);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void getTotalActiveRooms_shouldReturnSize() {
        given(examRoomRepository.findByIsActiveTrueOrderByRoomCodeAsc()).willReturn(List.of(new ExamRoom(), new ExamRoom()));

        int result = checker.getTotalActiveRooms();

        assertThat(result).isEqualTo(2);
    }
}
