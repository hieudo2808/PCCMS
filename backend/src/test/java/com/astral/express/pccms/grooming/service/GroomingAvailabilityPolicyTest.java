package com.astral.express.pccms.grooming.service;

import com.astral.express.pccms.appointment.repository.GroomingTicketRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GroomingAvailabilityPolicyTest {
    private final GroomingTicketRepository groomingTicketRepository = mock(GroomingTicketRepository.class);
    private final GroomingAvailabilityPolicy policy = new GroomingAvailabilityPolicy(groomingTicketRepository);

    @Test
    void shouldRejectUnavailableStation() {
        UUID stationId = UUID.randomUUID();
        UUID excludedTicketId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-17T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusHours(1);

        when(groomingTicketRepository.existsStationConflict(
                eq(stationId),
                anyCollection(),
                eq(startAt),
                eq(endAt),
                eq(excludedTicketId)
        )).thenReturn(true);

        assertThatThrownBy(() -> policy.requireStationAvailable(stationId, excludedTicketId, startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_006_STATION_UNAVAILABLE);
    }

    @Test
    void shouldRejectDuplicateOwnerBooking() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID serviceId = UUID.randomUUID();
        OffsetDateTime startAt = OffsetDateTime.parse("2026-06-17T09:00:00+07:00");
        OffsetDateTime endAt = startAt.plusHours(1);

        when(groomingTicketRepository.existsOwnerBookingConflict(
                eq(ownerId),
                eq(petId),
                eq(serviceId),
                anyCollection(),
                any(OffsetDateTime.class),
                any(OffsetDateTime.class)
        )).thenReturn(true);

        assertThatThrownBy(() -> policy.requireOwnerBookingAvailable(ownerId, petId, serviceId, startAt, endAt))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_GROOMING_009_DUPLICATE_BOOKING);
    }

}
