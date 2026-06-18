package com.astral.express.pccms.boarding.service;

import com.astral.express.pccms.boarding.entity.BoardingStatus;
import com.astral.express.pccms.boarding.entity.RoomAllocationStatus;
import com.astral.express.pccms.boarding.repository.BoardingBookingRepository;
import com.astral.express.pccms.boarding.repository.RoomAllocationRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BoardingAvailabilityPolicy {
    private static final List<BoardingStatus> OWNER_DUPLICATE_BLOCKING_STATUSES = List.of(
            BoardingStatus.RESERVED,
            BoardingStatus.CHECKED_IN,
            BoardingStatus.IN_STAY);

    private final BoardingBookingRepository boardingBookingRepository;
    private final RoomAllocationRepository roomAllocationRepository;

    public void requireOwnerBookingAvailable(UUID ownerId, UUID petId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (boardingBookingRepository.existsOwnerPetBookingConflict(
                ownerId,
                petId,
                OWNER_DUPLICATE_BLOCKING_STATUSES,
                startAt,
                endAt)) {
            throw new BusinessException(ErrorCode.ERR_BOARDING_006_DUPLICATE_BOOKING);
        }
    }

    public void requireRoomAvailable(UUID roomId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (roomAllocationRepository.existsActiveConflict(
                roomId,
                RoomAllocationStatus.ALLOCATED,
                startAt,
                endAt)) {
            throw new BusinessException(ErrorCode.ERR_ROOM_003_ROOM_UNAVAILABLE);
        }
    }
}
