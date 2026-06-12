package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.CreateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.dto.request.UpdateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineUsageTemplateResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineUsageTemplate;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.repository.MedicineUsageTemplateRepository;
import com.astral.express.pccms.medicine.service.MedicineUsageTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicineUsageTemplateService {

    private final MedicineRepository medicineRepository;
    private final MedicineUsageTemplateRepository templateRepository;
@Transactional
    public MedicineUsageTemplateResponse createTemplate(UUID medicineId, CreateMedicineUsageTemplateRequest request) {
        Medicine medicine = medicineRepository.findById(medicineId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND));

        if (templateRepository.existsByLabelAndMedicineId(request.label(), medicineId)) {
            throw new BusinessException(ErrorCode.ERR_MED_012_TEMPLATE_NAME_EXISTS);
        }

        MedicineUsageTemplate template = MedicineUsageTemplate.builder()
                .medicine(medicine)
                .label(request.label())
                .dosage(request.dosage())
                .frequency(request.frequency())
                .durationDays(request.durationDays())
                .instruction(request.instruction())
                .isDefault(request.isDefault())
                .sortOrder(request.sortOrder())
                .isActive(true)
                .build();

        return toResponse(templateRepository.save(template));
    }
@Transactional
    public MedicineUsageTemplateResponse updateTemplate(UUID medicineId, UUID templateId, UpdateMedicineUsageTemplateRequest request) {
        MedicineUsageTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getMedicine().getId().equals(medicineId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_011_TEMPLATE_NOT_FOUND));

        if (templateRepository.existsByLabelAndMedicineIdAndIdNot(request.label(), medicineId, templateId)) {
            throw new BusinessException(ErrorCode.ERR_MED_012_TEMPLATE_NAME_EXISTS);
        }

        template.setLabel(request.label());
        template.setDosage(request.dosage());
        template.setFrequency(request.frequency());
        template.setDurationDays(request.durationDays());
        template.setInstruction(request.instruction());
        template.setIsDefault(request.isDefault());
        template.setSortOrder(request.sortOrder());
        template.setIsActive(request.isActive());

        return toResponse(templateRepository.save(template));
    }
@Transactional
    public void deleteTemplate(UUID medicineId, UUID templateId) {
        MedicineUsageTemplate template = templateRepository.findById(templateId)
                .filter(t -> t.getMedicine().getId().equals(medicineId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_MED_011_TEMPLATE_NOT_FOUND));

        template.setIsActive(false);
        templateRepository.save(template);
    }
public List<MedicineUsageTemplateResponse> listByMedicine(UUID medicineId) {
        return templateRepository.findByMedicineIdAndIsActiveTrueOrderBySortOrderAsc(medicineId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private MedicineUsageTemplateResponse toResponse(MedicineUsageTemplate template) {
        return new MedicineUsageTemplateResponse(
                template.getId(),
                template.getMedicine().getId(),
                template.getLabel(),
                template.getDosage(),
                template.getFrequency(),
                template.getDurationDays(),
                template.getInstruction(),
                template.getIsDefault(),
                template.getSortOrder(),
                template.getIsActive()
        );
    }
}


