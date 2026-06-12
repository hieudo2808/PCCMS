package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.schedule.dto.request.WeeklySchedulePlanRequest;
import com.astral.express.pccms.schedule.dto.request.WorkScheduleRequest;
import com.astral.express.pccms.schedule.dto.response.WeeklySchedulePlanResponse;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.schedule.service.WorkScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/work-schedules")
@RequiredArgsConstructor
public class WorkScheduleController {
    private final WorkScheduleService workScheduleService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<PageResponse<WorkScheduleResponse>> searchSchedules(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @PageableDefault(size = 20, sort = "workDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(workScheduleService.searchSchedules(fromDate, toDate, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<WorkScheduleResponse> createSchedule(@Valid @RequestBody WorkScheduleRequest request) {
        return ApiResponse.success(workScheduleService.createSchedule(request));
    }

    @PostMapping("/weekly-plan/preview")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<WeeklySchedulePlanResponse> previewWeeklyPlan(
            @Valid @RequestBody WeeklySchedulePlanRequest request) {
        return ApiResponse.success(workScheduleService.previewWeeklyPlan(request));
    }

    @PostMapping("/weekly-plan/apply")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<WeeklySchedulePlanResponse> applyWeeklyPlan(
            @Valid @RequestBody WeeklySchedulePlanRequest request) {
        return ApiResponse.success(workScheduleService.applyWeeklyPlan(request));
    }

    @PutMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<WorkScheduleResponse> updateSchedule(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody WorkScheduleRequest request) {
        return ApiResponse.success(workScheduleService.updateSchedule(scheduleId, request));
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<WorkScheduleResponse> cancelSchedule(@PathVariable UUID scheduleId) {
        return ApiResponse.success(workScheduleService.cancelSchedule(scheduleId));
    }
}
