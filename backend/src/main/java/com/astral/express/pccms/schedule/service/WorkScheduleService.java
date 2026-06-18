package com.astral.express.pccms.schedule.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanResponse;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkScheduleService {
    private final WorkScheduleCrudService workScheduleCrudService;
    private final WeeklySchedulePlanner weeklySchedulePlanner;

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public PageResponse<WorkScheduleResponse> searchSchedules(
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable) {
        return workScheduleCrudService.searchSchedules(fromDate, toDate, pageable);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse createSchedule(WorkScheduleRequest request) {
        return workScheduleCrudService.createSchedule(request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse updateSchedule(UUID scheduleId, WorkScheduleRequest request) {
        return workScheduleCrudService.updateSchedule(scheduleId, request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WorkScheduleResponse cancelSchedule(UUID scheduleId) {
        return workScheduleCrudService.cancelSchedule(scheduleId);
    }

    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WeeklySchedulePlanResponse previewWeeklyPlan(WeeklySchedulePlanRequest request) {
        return weeklySchedulePlanner.previewWeeklyPlan(request);
    }

    @Transactional
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public WeeklySchedulePlanResponse applyWeeklyPlan(WeeklySchedulePlanRequest request) {
        return weeklySchedulePlanner.applyWeeklyPlan(request);
    }
}
