package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleConflictChecker {
    private final WorkScheduleRepository workScheduleRepository;

    public boolean hasStaffShiftConflict(UUID staffId, LocalDate workDate, UUID shiftId) {
        return workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(staffId, workDate, shiftId);
    }

    public boolean hasStaffShiftConflictExcluding(UUID staffId, LocalDate workDate, UUID shiftId, UUID scheduleId) {
        return workScheduleRepository.existsByStaffIdAndWorkDateAndShiftIdAndIdNot(
                staffId,
                workDate,
                shiftId,
                scheduleId
        );
    }
}
