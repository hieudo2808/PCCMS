package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.request.UpsertCareLogRequest;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.dto.response.StaffBoardingStayResponse;
import com.astral.express.pccms.boarding.service.BoardingStaffService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.identity.security.SecurityContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/boarding/staff")
@RequiredArgsConstructor
public class BoardingStaffController {

    private final BoardingStaffService boardingStaffService;
    private final SecurityContextService SecurityContextService;

    @GetMapping("/stays")
    @PreAuthorize("hasAuthority('BOARDING_READ')")
    public ApiResponse<List<StaffBoardingStayResponse>> listActiveStays() {
        return ApiResponse.success(boardingStaffService.listActiveStays());
    }

    @GetMapping("/care-logs")
    @PreAuthorize("hasAuthority('BOARDING_READ')")
    public ApiResponse<List<CareLogResponse>> listSessionLogs(
            @RequestParam UUID sessionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate logDate) {
        return ApiResponse.success(boardingStaffService.listSessionLogs(sessionId, logDate));
    }

    @PostMapping("/care-logs")
    @PreAuthorize("hasAuthority('BOARDING_UPDATE')")
    public ApiResponse<CareLogResponse> upsertCareLog(@Valid @RequestBody UpsertCareLogRequest request) {
        UUID staffId = SecurityContextService.getCurrentUserId();
        return ApiResponse.success(boardingStaffService.upsertCareLog(staffId, request));
    }
}
