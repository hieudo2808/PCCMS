package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.response.PrescriptionResponse;
import com.astral.express.pccms.medicalrecord.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/medical-records")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @GetMapping("/{id}/prescriptions")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ApiResponse<List<PrescriptionResponse>> listPrescriptions(@PathVariable("id") UUID medicalRecordId) {
        return ApiResponse.success(prescriptionService.listPrescriptions(medicalRecordId));
    }

    @PostMapping("/{id}/prescriptions")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
            @PathVariable("id") UUID medicalRecordId,
            @Valid @RequestBody CreatePrescriptionRequest request) {

        PrescriptionResponse response = prescriptionService.createPrescription(medicalRecordId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }
}
