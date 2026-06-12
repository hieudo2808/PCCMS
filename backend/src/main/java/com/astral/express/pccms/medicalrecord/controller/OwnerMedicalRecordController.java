package com.astral.express.pccms.medicalrecord.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse;
import com.astral.express.pccms.medicalrecord.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/owner/pets/{petId}/medical-records")
@RequiredArgsConstructor
public class OwnerMedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final SecurityContextService securityContextService;

    @GetMapping
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<List<MedicalRecordOwnerResponse>> getOwnerMedicalRecords(@PathVariable UUID petId) {
        UUID currentUserId = securityContextService.getCurrentUserId();
        List<MedicalRecordOwnerResponse> response = medicalRecordService.getOwnerMedicalRecords(petId, currentUserId);
        return ApiResponse.success(response);
    }
}
