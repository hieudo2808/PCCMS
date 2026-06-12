package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicalrecord.dto.request.CreateLabResultRequest;
import com.astral.express.pccms.medicalrecord.dto.response.LabResultResponse;
import com.astral.express.pccms.medicalrecord.service.LabResultService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/medical-records/{medicalRecordId}/lab-results")
@RequiredArgsConstructor
public class LabResultController {
    private final LabResultService labResultService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('MEDICAL_RECORD_UPDATE', 'APPOINTMENT_READ') or hasRole('ADMIN')")
    public ApiResponse<List<LabResultResponse>> listLabResults(@PathVariable UUID medicalRecordId) {
        return ApiResponse.success(labResultService.listLabResults(medicalRecordId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MEDICAL_RECORD_UPDATE')")
    public ApiResponse<LabResultResponse> createLabResult(
            @PathVariable UUID medicalRecordId,
            @Valid @RequestBody CreateLabResultRequest request) {
        return ApiResponse.created(labResultService.createLabResult(medicalRecordId, request));
    }
}
