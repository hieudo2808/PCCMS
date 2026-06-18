package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.entity.GroomingStatus;
import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GroomingAvailabilityPolicy {
    private static final List<GroomingStatus> STATION_BLOCKING_STATUSES = List.of(
            GroomingStatus.CONFIRMED,
            GroomingStatus.IN_SERVICE);
    private static final List<GroomingStatus> OWNER_DUPLICATE_BLOCKING_STATUSES = List.of(
            GroomingStatus.PENDING,
            GroomingStatus.CONFIRMED,
            GroomingStatus.IN_SERVICE);

    private final GroomingTicketRepository groomingTicketRepository;

    public void requireStationAvailable(UUID stationId, UUID excludedTicketId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (groomingTicketRepository.existsStationConflict(
                stationId,
                STATION_BLOCKING_STATUSES,
                startAt,
                endAt,
                excludedTicketId)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_006_STATION_UNAVAILABLE);
        }
    }

    public void requireOwnerBookingAvailable(UUID ownerId, UUID petId, UUID serviceId, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (groomingTicketRepository.existsOwnerBookingConflict(
                ownerId,
                petId,
                serviceId,
                OWNER_DUPLICATE_BLOCKING_STATUSES,
                startAt,
                endAt)) {
            throw new BusinessException(ErrorCode.ERR_GROOMING_009_DUPLICATE_BOOKING);
        }
    }
}
