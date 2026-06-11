package com.astral.express.pccms.appointment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentAvailabilityService {
    private final VetAvailabilityChecker vetAvailabilityChecker;
    private final RoomAvailabilityChecker roomAvailabilityChecker;
    private final AppointmentOverlapChecker overlapChecker;

    public boolean isSlotAvailable(OffsetDateTime startAt, OffsetDateTime endAt, UUID requestedVetId) {
        if (roomAvailabilityChecker.countFreeRooms(startAt, endAt) == 0) {
            return false;
        }

        if (requestedVetId != null) {
            return vetAvailabilityChecker.isVetOnDuty(startAt.toLocalDate(), startAt.toLocalTime(), requestedVetId)
                    && !overlapChecker.hasVetOverlap(requestedVetId, startAt, endAt);
        }

        List<UUID> vetCandidates = vetAvailabilityChecker.resolveVetCandidates(startAt.toLocalDate(), startAt.toLocalTime());
        if (vetCandidates.isEmpty()) {
            return roomAvailabilityChecker.countFreeRooms(startAt, endAt) > 0;
        }

        return vetCandidates.stream()
                .anyMatch(vetId -> !overlapChecker.hasVetOverlap(vetId, startAt, endAt));
    }
}
