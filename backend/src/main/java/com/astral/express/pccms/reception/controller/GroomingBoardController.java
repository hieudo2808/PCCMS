package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.reception.dto.request.GroomingStatusUpdateRequest;
import com.astral.express.pccms.reception.dto.response.GroomingTicketResponse;
import com.astral.express.pccms.reception.service.GroomingBoardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reception/grooming-tickets")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class GroomingBoardController {
    private final GroomingBoardService groomingBoardService;

    @GetMapping
    public ApiResponse<List<GroomingTicketResponse>> listTickets(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(groomingBoardService.listTickets(q, status), "Lấy bảng dịch vụ làm đẹp thành công");
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<GroomingTicketResponse> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody GroomingStatusUpdateRequest request) {
        return ApiResponse.success(groomingBoardService.updateStatus(id, request), "Cập nhật trạng thái làm đẹp thành công");
    }
}
