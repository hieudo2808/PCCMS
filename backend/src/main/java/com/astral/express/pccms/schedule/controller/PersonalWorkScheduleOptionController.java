package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.schedule.dto.response.StaffOptionResponse;
import com.astral.express.pccms.schedule.service.WorkScheduleOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/work-schedules/options")
@RequiredArgsConstructor
public class PersonalWorkScheduleOptionController {
    private final WorkScheduleOptionService workScheduleOptionService;

    @GetMapping("/staff")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE') or hasRole('STAFF') or hasRole('VETERINARIAN')")
    public ApiResponse<List<StaffOptionResponse>> getStaffOptions() {
        return ApiResponse.success(workScheduleOptionService.getStaffOptions());
    }
}
