package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCategoryUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.service.MedicineCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MedicineCategoryService {

    private final MedicineCategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
@Transactional
    public MedicineCategoryResponse create(MedicineCategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS);
        }
        MedicineCategory category = new MedicineCategory();
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }
@Transactional
    public MedicineCategoryResponse update(UUID id, MedicineCategoryUpdateRequest request) {
        MedicineCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
        if (categoryRepository.existsByNameAndIdNot(request.name(), id)) {
            throw new BusinessException(ErrorCode.ERR_MED_009_CATEGORY_NAME_EXISTS);
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setIsActive(request.isActive());
        return toResponse(categoryRepository.save(category));
    }
@Transactional(readOnly = true)
    public MedicineCategoryResponse getById(UUID id) {
        return categoryRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
    }
@Transactional(readOnly = true)
    public List<MedicineCategoryResponse> listAll() {
        return categoryRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }
@Transactional(readOnly = true)
    public List<MedicineCategoryResponse> listActive() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream().map(this::toResponse).toList();
    }
@Transactional
    public void delete(UUID id) {
        MedicineCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
        if (medicineRepository.existsByCategoryId(id)) {
            throw new BusinessException(ErrorCode.ERR_MED_010_CATEGORY_IN_USE);
        }
        category.setIsActive(false);
        categoryRepository.save(category);
    }

    private MedicineCategoryResponse toResponse(MedicineCategory category) {
        return new MedicineCategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getIsActive()
        );
    }
}


