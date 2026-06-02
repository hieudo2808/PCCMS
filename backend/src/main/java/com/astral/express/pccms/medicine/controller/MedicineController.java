package com.astral.express.pccms.medicine.controller;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.dto.ApiResponse;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.service.MedicineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/medicines")
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PostMapping
    public ApiResponse<MedicineResponse> createMedicine(@Valid @RequestBody MedicineCreateRequest request) {
        return ApiResponse.success(medicineService.createMedicine(request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PutMapping("/{id}")
    public ApiResponse<MedicineResponse> updateMedicine(
            @PathVariable UUID id,
            @Valid @RequestBody MedicineUpdateRequest request) {
        return ApiResponse.success(medicineService.updateMedicine(id, request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('VETERINARIAN')")
    @GetMapping("/{id}")
    public ApiResponse<MedicineResponse> getMedicine(@PathVariable UUID id) {
        return ApiResponse.success(medicineService.getMedicine(id));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('VETERINARIAN')")
    @GetMapping
    public ApiResponse<PageResponse<MedicineResponse>> getAllMedicines(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(medicineService.getAllMedicines(pageable));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @PatchMapping("/{id}/stock")
    public ApiResponse<MedicineResponse> addStock(
            @PathVariable UUID id,
            @Valid @RequestBody AddStockRequest request) {
        return ApiResponse.success(medicineService.addStock(id, request));
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMedicine(@PathVariable UUID id) {
        medicineService.deleteMedicine(id);
        return ApiResponse.success(null);
    }
}
