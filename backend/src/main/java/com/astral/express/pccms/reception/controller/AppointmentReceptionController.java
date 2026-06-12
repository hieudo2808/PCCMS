package com.astral.express.pccms.reception.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.reception.dto.request.AppointmentCancelRequest;
import com.astral.express.pccms.reception.dto.request.AppointmentReceiveRequest;
import com.astral.express.pccms.reception.dto.request.QuickAppointmentRequest;
import com.astral.express.pccms.reception.dto.response.AppointmentReceptionResponse;
import com.astral.express.pccms.reception.service.AppointmentReceptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/reception/appointments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
public class AppointmentReceptionController {
    private final AppointmentReceptionService appointmentReceptionService;

    @GetMapping
    public ApiResponse<List<AppointmentReceptionResponse>> listAppointments(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(appointmentReceptionService.listAppointments(q, status), "Lấy danh sách lịch hẹn thành công");
    }

    @PostMapping("/quick")
    public ApiResponse<AppointmentReceptionResponse> quickCreateAndReceive(@Valid @RequestBody QuickAppointmentRequest request) {
        return ApiResponse.success(appointmentReceptionService.quickCreateAndReceive(request), "Tạo nhanh và tiếp nhận thành công");
    }

    @PatchMapping("/{id}/receive")
    public ApiResponse<AppointmentReceptionResponse> receive(
            @PathVariable UUID id,
            @RequestBody(required = false) AppointmentReceiveRequest request) {
        return ApiResponse.success(appointmentReceptionService.receive(id, request), "Tiếp nhận thành công");
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<AppointmentReceptionResponse> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) AppointmentCancelRequest request) {
        return ApiResponse.success(appointmentReceptionService.cancel(id, request), "Hủy lịch hẹn thành công");
    }
}
