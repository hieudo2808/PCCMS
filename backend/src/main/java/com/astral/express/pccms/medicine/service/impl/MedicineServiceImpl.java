package com.astral.express.pccms.medicine.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineCategoryResponse;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.mapper.MedicineMapper;
import com.astral.express.pccms.medicalrecord.repository.PrescriptionItemRepository;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional
    public MedicineResponse createMedicine(MedicineCreateRequest request) {
        validatePricingAndStock(request.currentStock(), request.unitPriceVnd());
        ensureUniqueCode(request.medicineCode(), null);
        ensureUniqueNameAndUnit(request.name(), request.unit(), null);

        Medicine medicine = medicineMapper.toMedicine(request);
        medicine.setCategory(resolveCategory(request.categoryId()));

        medicine = medicineRepository.save(medicine);
        log.info("Created new medicine: {}", medicine.getId());
        return medicineMapper.toMedicineResponse(medicine);
    }

    @Override
    @Transactional
    public MedicineResponse updateMedicine(UUID id, MedicineUpdateRequest request) {
        validatePricingAndStock(request.currentStock(), request.unitPriceVnd());

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        ensureUniqueCode(request.medicineCode(), id);
        ensureUniqueNameAndUnit(request.name(), request.unit(), id);

        medicineMapper.updateMedicineFromRequest(request, medicine);
        medicine.setMedicineCode(request.medicineCode());
        if (request.categoryId() != null) {
            medicine.setCategory(resolveCategory(request.categoryId()));
        }

        medicine = medicineRepository.save(medicine);
        log.info("Updated medicine: {}", medicine.getId());
        return medicineMapper.toMedicineResponse(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicineResponse getMedicine(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));
        return medicineMapper.toMedicineResponse(medicine);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> getAllMedicines(UUID categoryId, Pageable pageable) {
        Page<Medicine> page = categoryId != null
                ? medicineRepository.findByCategoryId(categoryId, pageable)
                : medicineRepository.findAll(pageable);
        return PageResponse.of(page.map(medicineMapper::toMedicineResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MedicineCategoryResponse> listCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(cat -> new MedicineCategoryResponse(cat.getId(), cat.getName(), cat.getDescription(), cat.getIsActive()))
                .toList();
    }

    @Override
    @Transactional
    public void deleteMedicine(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        if (prescriptionItemRepository.existsByMedicineId(id)) {
            throw new BusinessException(ErrorCode.ERR_MED_008_MEDICINE_IN_USE);
        }

        medicine.setIsActive(false);
        medicineRepository.save(medicine);
        log.info("Deleted (soft) medicine: {}", id);
    }

    @Override
    @Transactional
    public MedicineResponse addStock(UUID id, AddStockRequest request) {
        if (request.quantityToAdd() <= 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        Medicine medicine = medicineRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        medicine.setCurrentStock(medicine.getCurrentStock() + request.quantityToAdd());
        medicine = medicineRepository.save(medicine);
        log.info("Added {} stock to medicine: {}", request.quantityToAdd(), id);
        return medicineMapper.toMedicineResponse(medicine);
    }

    private MedicineCategory resolveCategory(UUID categoryId) {
        if (categoryId == null) {
            throw new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
    }

    private void validatePricingAndStock(Integer stock, java.math.BigDecimal price) {
        if (stock < 0 || price.compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
    }

    private void ensureUniqueCode(String medicineCode, UUID excludeId) {
        boolean exists = excludeId == null
                ? medicineRepository.existsByMedicineCode(medicineCode)
                : medicineRepository.existsByMedicineCodeAndIdNot(medicineCode, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_MED_005_MEDICINE_CODE_EXISTS);
        }
    }

    private void ensureUniqueNameAndUnit(String name, String unit, UUID excludeId) {
        boolean exists = excludeId == null
                ? medicineRepository.existsByNameAndUnit(name, unit)
                : medicineRepository.existsByNameAndUnitAndIdNot(name, unit, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_MED_007_MEDICINE_NAME_EXISTS);
        }
    }
}
