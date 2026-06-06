package com.astral.express.pccms.catalog.controller;

import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;
import com.astral.express.pccms.medicine.service.MedicineCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/catalog/medicine-categories")
@RequiredArgsConstructor
public class MedicineCategoryController {

    private final MedicineCategoryService medicineCategoryService;

    @PreAuthorize("hasAuthority('MEDICINE_MANAGE')")
    @PostMapping
    public ApiResponse<MedicineCategoryResponse> create(@Valid @RequestBody MedicineCategoryCreateRequest request) {
        return ApiResponse.created(medicineCategoryService.create(request));
    }

    @PreAuthorize("hasAuthority('MEDICINE_MANAGE')")
    @PutMapping("/{id}")
    public ApiResponse<MedicineCategoryResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody MedicineCategoryUpdateRequest request) {
        return ApiResponse.success(medicineCategoryService.update(id, request));
    }

    @PreAuthorize("hasAuthority('MEDICINE_MANAGE') or hasAuthority('PRESCRIPTION_CREATE')")
    @GetMapping("/{id}")
    public ApiResponse<MedicineCategoryResponse> getById(@PathVariable UUID id) {
        return ApiResponse.success(medicineCategoryService.getById(id));
    }

    @PreAuthorize("hasAuthority('MEDICINE_MANAGE') or hasAuthority('PRESCRIPTION_CREATE')")
    @GetMapping
    public ApiResponse<List<MedicineCategoryResponse>> list(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ApiResponse.success(activeOnly
                ? medicineCategoryService.listActive()
                : medicineCategoryService.listAll());
    }

    @PreAuthorize("hasAuthority('MEDICINE_MANAGE')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable UUID id) {
        medicineCategoryService.delete(id);
        return ApiResponse.success(null);
    }
}
