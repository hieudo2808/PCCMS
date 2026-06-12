package com.astral.express.pccms.schedule.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.schedule.dto.request.ShiftRequestStatusUpdateRequest;
import com.astral.express.pccms.schedule.dto.response.ShiftChangeRequestResponse;
import com.astral.express.pccms.schedule.service.ShiftChangeRequestService;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.schedule.entity.ShiftRequestStatus;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/shift-change-requests")
@RequiredArgsConstructor
public class AdminShiftChangeRequestController {
    private final ShiftChangeRequestService shiftChangeRequestService;

    @GetMapping
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<PageResponse<ShiftChangeRequestResponse>> getAdminRequests(
            @RequestParam(required = false) ShiftRequestStatus statusCode,
            @PageableDefault(size = 20, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        return ApiResponse.success(shiftChangeRequestService.getAdminRequests(statusCode, pageable));
    }

    @PatchMapping("/{requestId}/status")
    @PreAuthorize("hasAuthority('SCHEDULE_MANAGE')")
    public ApiResponse<ShiftChangeRequestResponse> updateRequestStatus(
            @PathVariable UUID requestId,
            @Valid @RequestBody ShiftRequestStatusUpdateRequest request) {
        return ApiResponse.success(shiftChangeRequestService.updateRequestStatus(requestId, request.statusCode()));
    }
}
