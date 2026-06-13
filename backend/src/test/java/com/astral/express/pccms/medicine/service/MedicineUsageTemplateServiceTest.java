package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.CreateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.dto.request.UpdateMedicineUsageTemplateRequest;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineUsageTemplate;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.repository.MedicineUsageTemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicineUsageTemplateServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineUsageTemplateRepository templateRepository;

    @InjectMocks
    private MedicineUsageTemplateService templateService;

    @Test
    void should_createTemplate_success() {
        UUID medicineId = UUID.randomUUID();
        CreateMedicineUsageTemplateRequest request = new CreateMedicineUsageTemplateRequest("Morning", "1 pill", "1", 3, "After meal", false, 1);
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);

        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(medicine));
        given(templateRepository.existsByLabelAndMedicineId("Morning", medicineId)).willReturn(false);
        given(templateRepository.save(any())).willAnswer(inv -> {
            MedicineUsageTemplate t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });

        var res = templateService.createTemplate(medicineId, request);
        assertThat(res.label()).isEqualTo("Morning");
    }

    @Test
    void should_createTemplate_throwMedicineNotFound() {
        UUID medicineId = UUID.randomUUID();
        CreateMedicineUsageTemplateRequest request = new CreateMedicineUsageTemplateRequest("Morning", "1 pill", "1", 3, "After meal", false, 1);

        given(medicineRepository.findById(medicineId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.createTemplate(medicineId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_004_MEDICINE_NOT_FOUND);
    }

    @Test
    void should_createTemplate_throwNameExists() {
        UUID medicineId = UUID.randomUUID();
        CreateMedicineUsageTemplateRequest request = new CreateMedicineUsageTemplateRequest("Morning", "1 pill", "1", 3, "After meal", false, 1);
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);

        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(medicine));
        given(templateRepository.existsByLabelAndMedicineId("Morning", medicineId)).willReturn(true);

        assertThatThrownBy(() -> templateService.createTemplate(medicineId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_012_TEMPLATE_NAME_EXISTS);
    }

    @Test
    void should_updateTemplate_success() {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UpdateMedicineUsageTemplateRequest request = new UpdateMedicineUsageTemplateRequest("Night", "2 pills", "1", 5, "Before sleep", true, 2, true);
        
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        MedicineUsageTemplate template = new MedicineUsageTemplate();
        template.setId(templateId);
        template.setMedicine(medicine);

        given(templateRepository.findById(templateId)).willReturn(Optional.of(template));
        given(templateRepository.existsByLabelAndMedicineIdAndIdNot("Night", medicineId, templateId)).willReturn(false);
        given(templateRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        var res = templateService.updateTemplate(medicineId, templateId, request);
        assertThat(res.label()).isEqualTo("Night");
    }

    @Test
    void should_updateTemplate_throwNotFound() {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UpdateMedicineUsageTemplateRequest request = new UpdateMedicineUsageTemplateRequest("Night", "2 pills", "1", 5, "Before sleep", true, 2, true);

        given(templateRepository.findById(templateId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.updateTemplate(medicineId, templateId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_011_TEMPLATE_NOT_FOUND);
    }

    @Test
    void should_updateTemplate_throwNameExists() {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        UpdateMedicineUsageTemplateRequest request = new UpdateMedicineUsageTemplateRequest("Night", "2 pills", "1", 5, "Before sleep", true, 2, true);
        
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        MedicineUsageTemplate template = new MedicineUsageTemplate();
        template.setId(templateId);
        template.setMedicine(medicine);

        given(templateRepository.findById(templateId)).willReturn(Optional.of(template));
        given(templateRepository.existsByLabelAndMedicineIdAndIdNot("Night", medicineId, templateId)).willReturn(true);

        assertThatThrownBy(() -> templateService.updateTemplate(medicineId, templateId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_012_TEMPLATE_NAME_EXISTS);
    }

    @Test
    void should_deleteTemplate_success() {
        UUID medicineId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        MedicineUsageTemplate template = new MedicineUsageTemplate();
        template.setId(templateId);
        template.setMedicine(medicine);
        template.setIsActive(true);

        given(templateRepository.findById(templateId)).willReturn(Optional.of(template));

        templateService.deleteTemplate(medicineId, templateId);

        verify(templateRepository).save(template);
        assertThat(template.getIsActive()).isFalse();
    }

    @Test
    void should_listByMedicine_success() {
        UUID medicineId = UUID.randomUUID();
        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        MedicineUsageTemplate template = new MedicineUsageTemplate();
        template.setId(UUID.randomUUID());
        template.setMedicine(medicine);

        given(templateRepository.findByMedicineIdAndIsActiveTrueOrderBySortOrderAsc(medicineId)).willReturn(List.of(template));

        var res = templateService.listByMedicine(medicineId);
        assertThat(res).hasSize(1);
    }
}
