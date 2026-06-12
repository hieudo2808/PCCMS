package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.schedule.dto.response.WorkScheduleResponse;
import com.astral.express.pccms.schedule.service.PersonalScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/me/work-schedules")
@RequiredArgsConstructor
public class PersonalScheduleController {
    private final PersonalScheduleService personalScheduleService;

    @GetMapping
    public ApiResponse<PageResponse<WorkScheduleResponse>> getMySchedules(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @PageableDefault(size = 20, sort = "workDate", direction = Sort.Direction.ASC) Pageable pageable) {
        return ApiResponse.success(personalScheduleService.getMySchedules(fromDate, toDate, pageable));
    }
}
