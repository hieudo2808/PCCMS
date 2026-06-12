package com.astral.express.pccms.medicine.service;

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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final PrescriptionItemRepository prescriptionItemRepository;
    private final MedicineMapper medicineMapper;
@Transactional(readOnly = true)
    public PageResponse<MedicineResponse> searchMedicines(
            String keyword,
            UUID categoryId,
            Boolean isActive,
            Pageable pageable) {
        Specification<Medicine> specification = combine(
                keywordContains(keyword),
                categoryEquals(categoryId),
                activeEquals(isActive)
        );
        Page<Medicine> page = medicineRepository.findAll(specification, pageable);
        return PageResponse.of(page.map(medicineMapper::toMedicineResponse));
    }
@Transactional
    public MedicineResponse createMedicine(MedicineCreateRequest request) {
        if (request.currentStock() < 0 || request.unitPriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        
        Medicine medicine = medicineMapper.toMedicine(request);
        medicine.setMedicineCode(resolveMedicineCode(request.medicineCode(), request.name()));
        ensureUniqueNameAndUnit(request.name(), request.unit(), null);
        medicine.setCategory(resolveCategory(request.categoryId()));

        medicine = medicineRepository.save(medicine);
        log.info("Created new medicine: {}", medicine.getId());
        return medicineMapper.toMedicineResponse(medicine);
    }
@Transactional
    public MedicineResponse updateMedicine(UUID id, MedicineUpdateRequest request) {
        if (request.currentStock() < 0 || request.unitPriceVnd() < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        String medicineCode = request.medicineCode() == null || request.medicineCode().isBlank()
                ? medicine.getMedicineCode()
                : normalizeCode(request.medicineCode());
        ensureUniqueCode(medicineCode, id);
        ensureUniqueNameAndUnit(request.name(), request.unit(), id);

        medicineMapper.updateMedicineFromRequest(request, medicine);
        medicine.setMedicineCode(medicineCode);
        medicine.setCategory(resolveCategory(request.categoryId()));

        medicine = medicineRepository.save(medicine);
        log.info("Updated medicine: {}", medicine.getId());
        return medicineMapper.toMedicineResponse(medicine);
    }
@Transactional(readOnly = true)
    public MedicineResponse getMedicine(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));
        return medicineMapper.toMedicineResponse(medicine);
    }
@Transactional(readOnly = true)
    public PageResponse<MedicineResponse> getAllMedicines(UUID categoryId, Pageable pageable) {
        Page<Medicine> page = categoryId != null
                ? medicineRepository.findByCategoryId(categoryId, pageable)
                : medicineRepository.findAll(pageable);
        return PageResponse.of(page.map(medicineMapper::toMedicineResponse));
    }
@Transactional(readOnly = true)
    public List<MedicineCategoryResponse> listCategories() {
        return categoryRepository.findByIsActiveTrueOrderByNameAsc().stream()
                .map(cat -> new MedicineCategoryResponse(cat.getId(), cat.getName(), cat.getDescription(), cat.getIsActive()))
                .toList();
    }
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

    private void ensureUniqueCode(String medicineCode, UUID excludeId) {
        boolean exists = excludeId == null
                ? medicineRepository.existsByMedicineCodeIgnoreCase(medicineCode)
                : medicineRepository.existsByMedicineCodeIgnoreCaseAndIdNot(medicineCode, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_MED_005_MEDICINE_CODE_EXISTS);
        }
    }

    private void ensureUniqueNameAndUnit(String name, String unit, UUID excludeId) {
        boolean exists = excludeId == null
                ? medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCase(name, unit)
                : medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCaseAndIdNot(name, unit, excludeId);
        if (exists) {
            throw new BusinessException(ErrorCode.ERR_MED_007_MEDICINE_NAME_EXISTS);
        }
    }

    private String resolveMedicineCode(String requestedCode, String medicineName) {
        if (requestedCode != null && !requestedCode.isBlank()) {
            String normalized = normalizeCode(requestedCode);
            ensureUniqueCode(normalized, null);
            return normalized;
        }

        String prefix = normalizeCode(medicineName)
                .replaceAll("[^A-Z0-9]+", "")
                .replaceAll("^(.{0,8}).*$", "$1");
        if (prefix.isBlank()) {
            prefix = "MED";
        }

        for (int sequence = 1; sequence <= 9999; sequence++) {
            String candidate = "MED-" + prefix + "-" + String.format("%04d", sequence);
            if (!medicineRepository.existsByMedicineCodeIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(ErrorCode.ERR_MED_005_MEDICINE_CODE_EXISTS);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }

    private Specification<Medicine> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String normalizedKeyword = keyword.trim().toLowerCase();
        String pattern = "%" + normalizedKeyword + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("medicineCode")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), pattern)
        );
    }

    private Specification<Medicine> categoryEquals(UUID categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("category").get("id"), categoryId);
    }

    private Specification<Medicine> activeEquals(Boolean isActive) {
        if (isActive == null) {
            return null;
        }
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("isActive"), isActive);
    }

    @SafeVarargs
    private final Specification<Medicine> combine(Specification<Medicine>... specifications) {
        List<Specification<Medicine>> activeSpecifications = new ArrayList<>();
        for (Specification<Medicine> specification : specifications) {
            if (specification != null) {
                activeSpecifications.add(specification);
            }
        }
        if (activeSpecifications.isEmpty()) {
            return null;
        }
        Specification<Medicine> combined = activeSpecifications.get(0);
        for (int index = 1; index < activeSpecifications.size(); index++) {
            combined = combined.and(activeSpecifications.get(index));
        }
        return combined;
    }
}


