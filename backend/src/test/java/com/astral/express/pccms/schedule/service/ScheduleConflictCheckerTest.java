package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.schedule.repository.WorkScheduleRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleConflictCheckerTest {
    private final WorkScheduleRepository workScheduleRepository = mock(WorkScheduleRepository.class);
    private final ScheduleConflictChecker checker = new ScheduleConflictChecker(workScheduleRepository);

    @Test
    void shouldDetectStaffShiftConflict() {
        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 6, 17);

        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftId(staffId, workDate, shiftId))
                .thenReturn(true);

        assertThat(checker.hasStaffShiftConflict(staffId, workDate, shiftId)).isTrue();
    }

    @Test
    void shouldDetectStaffShiftConflictExcludingCurrentSchedule() {
        UUID staffId = UUID.randomUUID();
        UUID shiftId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        LocalDate workDate = LocalDate.of(2026, 6, 17);

        when(workScheduleRepository.existsByStaffIdAndWorkDateAndShiftIdAndIdNot(
                staffId,
                workDate,
                shiftId,
                scheduleId
        )).thenReturn(true);

        assertThat(checker.hasStaffShiftConflictExcluding(staffId, workDate, shiftId, scheduleId)).isTrue();
    }
}
