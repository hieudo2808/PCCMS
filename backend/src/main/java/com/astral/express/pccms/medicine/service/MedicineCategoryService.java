package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.medicine.dto.request.MedicineCategoryCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;

import java.util.List;
import java.util.UUID;

public interface MedicineCategoryService {
    MedicineCategoryResponse create(MedicineCategoryCreateRequest request);
    MedicineCategoryResponse update(UUID id, MedicineCategoryUpdateRequest request);
    MedicineCategoryResponse getById(UUID id);
    List<MedicineCategoryResponse> listAll();
    List<MedicineCategoryResponse> listActive();
    void delete(UUID id);
}
