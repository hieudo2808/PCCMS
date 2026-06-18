package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.RoomAllocationRepository;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BoardingAvailabilityPolicyTest {
    private final BoardingBookingRepository boardingBookingRepository = mock(BoardingBookingRepository.class);
    private final RoomAllocationRepository roomAllocationRepository = mock(RoomAllocationRepository.class);
    private final BoardingAvailabilityPolicy policy = new BoardingAvailabilityPolicy(
            boardingBookingRepository,
            roomAllocationRepository
    );

    @Test
    void shouldRejectDuplicateOwnerBooking() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-17T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusDays(2);

        when(boardingBookingRepository.existsOwnerPetBookingConflict(
                eq(ownerId),
                eq(petId),
                anyCollection(),
                eq(startAt),
                eq(endAt)
        )).thenReturn(true);

        assertThatThrownBy(() -> policy.requireOwnerBookingAvailable(ownerId, petId, startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_BOARDING_006_DUPLICATE_BOOKING);
    }

    @Test
    void shouldRejectRoomAllocationConflict() {
        UUID roomId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-17T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusDays(2);

        when(roomAllocationRepository.existsActiveConflict(eq(roomId), eq(RoomAllocationStatus.ALLOCATED), eq(startAt), eq(endAt)))
                .thenReturn(true);

        assertThatThrownBy(() -> policy.requireRoomAvailable(roomId, startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
    }

    @Test
    void shouldAllowAvailableOwnerAndRoom() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID roomId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-17T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusDays(2);

        assertThatCode(() -> policy.requireOwnerBookingAvailable(ownerId, petId, startAt, endAt))
                .doesNotThrowAnyException();
        assertThatCode(() -> policy.requireRoomAvailable(roomId, startAt, endAt))
                .doesNotThrowAnyException();
    }
}
