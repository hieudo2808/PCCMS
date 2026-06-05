package com.astral.express.pccms.boarding.controller;

import com.astral.express.pccms.boarding.dto.response.BoardingStayResponse;
import com.astral.express.pccms.boarding.dto.response.CareLogResponse;
import com.astral.express.pccms.boarding.service.BoardingTrackingService;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.identity.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/boarding/owner")
@RequiredArgsConstructor
public class BoardingTrackingController {

    private final BoardingTrackingService boardingTrackingService;
    private final SecurityHelper securityHelper;

    @GetMapping("/stays")
    @PreAuthorize("hasAuthority('BOARDING_READ')")
    public ApiResponse<List<BoardingStayResponse>> listActiveStays() {
        UUID ownerId = securityHelper.getCurrentUserId();
        return ApiResponse.success(boardingTrackingService.listActiveStays(ownerId));
    }

    @GetMapping("/care-logs")
    @PreAuthorize("hasAuthority('BOARDING_READ')")
    public ApiResponse<List<CareLogResponse>> listCareLogs(
            @RequestParam(required = false) UUID petId) {
        UUID ownerId = securityHelper.getCurrentUserId();
        return ApiResponse.success(boardingTrackingService.listCareLogs(ownerId, petId));
    }
}
