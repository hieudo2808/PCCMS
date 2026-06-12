package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN') or hasRole('ADMIN')")
    public ApiResponse<MedicalRecordResponse> getMedicalRecordById(@PathVariable UUID id) {
        MedicalRecordResponse response = medicalRecordService.getMedicalRecordById(id);
        return ApiResponse.success(response);
    }

    @GetMapping("/appointment/{appointmentId}")
    @PreAuthorize("hasRole('VETERINARIAN') or hasRole('ADMIN')")
    public ApiResponse<MedicalRecordResponse> getOrCreateMedicalRecordByAppointmentId(
            @PathVariable UUID appointmentId) {
        MedicalRecordResponse response = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        return ApiResponse.success(response);
    }


    @GetMapping
    @PreAuthorize("hasRole('VETERINARIAN') or hasRole('ADMIN')")
    public ApiResponse<List<MedicalRecordResponse>> getMedicalRecords(
            @RequestParam(required = false) UUID vetId) {
        List<MedicalRecordResponse> response = medicalRecordService.getMedicalRecords(vetId);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<MedicalRecordResponse> updateMedicalRecord(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        MedicalRecordResponse response = medicalRecordService.updateMedicalRecord(id, request);
        return ApiResponse.success(response);
    }

    @PatchMapping("/{id}/finalize")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<MedicalRecordResponse> finalizeMedicalRecord(
            @PathVariable UUID id,
            @Valid @RequestBody FinalizeMedicalRecordRequest request) {
        MedicalRecordResponse response = medicalRecordService.finalizeMedicalRecord(id, request);
        return ApiResponse.success(response);
    }
}

