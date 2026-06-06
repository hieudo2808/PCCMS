package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MedicineService {
    MedicineResponse createMedicine(MedicineCreateRequest request);
    MedicineResponse updateMedicine(UUID id, MedicineUpdateRequest request);
    MedicineResponse getMedicine(UUID id);
    PageResponse<MedicineResponse> getAllMedicines(UUID categoryId, Pageable pageable);
    List<MedicineCategoryResponse> listCategories();
    void deleteMedicine(UUID id);
    MedicineResponse addStock(UUID id, AddStockRequest request);
}
