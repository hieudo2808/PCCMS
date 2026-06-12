package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicine.dto.request.CreateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.security.MedicinePermissions;
import org.springframework.security.access.prepost.PreAuthorize;
import com.astral.express.pccms.medicine.dto.request.UpdateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineUsageTemplateResponse;
import com.astral.express.pccms.medicine.service.MedicineUsageTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/medicines/{medicineId}/usage-templates")
@RequiredArgsConstructor
public class MedicineUsageTemplateController {
    private final MedicineUsageTemplateService service;

    @PreAuthorize(MedicinePermissions.MEDICINE_READ)
    @GetMapping
    public ApiResponse<List<MedicineUsageTemplateResponse>> listByMedicine(@PathVariable UUID medicineId) {
        return ApiResponse.success(service.listByMedicine(medicineId), "Lấy danh sách thành công");
    }

    @PreAuthorize(MedicinePermissions.MEDICINE_MANAGE)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MedicineUsageTemplateResponse> createTemplate(
            @PathVariable UUID medicineId,
            @Valid @RequestBody CreateMedicineUsageTemplateRequest request) {
        return ApiResponse.created(service.createTemplate(medicineId, request));
    }

    @PreAuthorize(MedicinePermissions.MEDICINE_MANAGE)
    @PutMapping("/{templateId}")
    public ApiResponse<MedicineUsageTemplateResponse> updateTemplate(
            @PathVariable UUID medicineId,
            @PathVariable UUID templateId,
            @Valid @RequestBody UpdateMedicineUsageTemplateRequest request) {
        return ApiResponse.success(service.updateTemplate(medicineId, templateId, request), "Cập nhật thành công");
    }

    @PreAuthorize(MedicinePermissions.MEDICINE_MANAGE)
    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(
            @PathVariable UUID medicineId,
            @PathVariable UUID templateId) {
        service.deleteTemplate(medicineId, templateId);
        return ApiResponse.success(null, "Xóa thành công");
    }
}
