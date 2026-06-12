package com.astral.express.pccms.appointment.service;

import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VetAvailabilityChecker {
    private static final String VET_ROLE = "VETERINARIAN";
    
    private final UserRepository userRepository;
    private final WorkScheduleRepository workScheduleRepository;
    private final AppointmentOverlapChecker overlapChecker;

    public boolean isVetOnDuty(LocalDate date, LocalTime slotStart, UUID vetId) {
        List<UUID> scheduledVetIds = workScheduleRepository.findAvailableVetIds(date, slotStart);
        if (scheduledVetIds.isEmpty()) {
            return userRepository.findActiveByRoleCode(VET_ROLE).stream()
                    .anyMatch(v -> v.getId().equals(vetId));
        }
        return scheduledVetIds.contains(vetId);
    }

    public List<UUID> resolveVetCandidates(LocalDate date, LocalTime slotStart) {
        List<UUID> scheduledVetIds = workScheduleRepository.findAvailableVetIds(date, slotStart);
        if (!scheduledVetIds.isEmpty()) {
            return scheduledVetIds;
        }
        return userRepository.findActiveByRoleCode(VET_ROLE).stream().map(Users::getId).toList();
    }

    public Users requireVetAvailable(LocalDate date, LocalTime slotStart, UUID requestedVetId,
                                     OffsetDateTime startAt, OffsetDateTime endAt) {
        if (requestedVetId != null) {
            Users vet = userRepository.findById(requestedVetId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
            if (!VET_ROLE.equals(vet.getRole().getCode())) {
                throw new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE);
            }
            if (!isVetOnDuty(date, slotStart, requestedVetId)
                    || overlapChecker.hasVetOverlap(requestedVetId, startAt, endAt)) {
                throw new BusinessException(ErrorCode.ERR_APT_009_SLOT_FULL);
            }
            return vet;
        }

        List<UUID> candidates = resolveVetCandidates(date, slotStart);
        return candidates.stream()
                .map(userRepository::findById)
                .flatMap(Optional::stream)
                .filter(v -> !overlapChecker.hasVetOverlap(v.getId(), startAt, endAt))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_APT_005_NO_VET_AVAILABLE));
    }

    public boolean isVetFree(UUID vetId, OffsetDateTime startAt, OffsetDateTime endAt) {
        return !overlapChecker.hasVetOverlap(vetId, startAt, endAt);
    }

    public List<UUID> findVetIdsOnDutyForDate(LocalDate date) {
        return workScheduleRepository.findVetIdsOnDutyForDate(date);
    }
}
