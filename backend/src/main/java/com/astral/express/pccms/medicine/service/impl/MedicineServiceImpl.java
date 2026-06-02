package com.astral.express.pccms.medicine.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.mapper.MedicineMapper;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.service.MedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicineServiceImpl implements MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final MedicineMapper medicineMapper;

    @Override
    @Transactional
    public MedicineResponse createMedicine(MedicineCreateRequest request) {
        if (request.currentStock() < 0 || request.unitPriceVnd().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }
        
        Medicine medicine = medicineMapper.toMedicine(request);
        
        if (request.categoryId() != null) {
            MedicineCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
            medicine.setCategory(category);
        }
        
        medicine = medicineRepository.save(medicine);
        log.info("Created new medicine: {}", medicine.getId());
        return medicineMapper.toMedicineResponse(medicine);
    }

    @Override
    @Transactional
    public MedicineResponse updateMedicine(UUID id, MedicineUpdateRequest request) {
        if (request.currentStock() < 0 || request.unitPriceVnd().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.ERR_VALIDATION_FAILED);
        }

        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        medicineMapper.updateMedicineFromRequest(request, medicine);
        
        if (request.categoryId() != null) {
            MedicineCategory category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND));
            medicine.setCategory(category);
        } else {
            medicine.setCategory(null);
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
    public PageResponse<MedicineResponse> getAllMedicines(Pageable pageable) {
        Page<Medicine> page = medicineRepository.findAll(pageable);
        return PageResponse.of(page.map(medicineMapper::toMedicineResponse));
    }

    @Override
    @Transactional
    public void deleteMedicine(UUID id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));
        
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
}
