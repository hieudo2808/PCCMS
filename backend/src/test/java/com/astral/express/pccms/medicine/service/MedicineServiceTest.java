package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.mapper.MedicineMapper;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineCategoryRepository categoryRepository;

    @Mock
    private MedicineMapper medicineMapper;

    @Mock
    private com.astral.express.pccms.medicalrecord.repository.PrescriptionItemRepository prescriptionItemRepository;

    @InjectMocks
    private MedicineService medicineService;

    @ParameterizedTest(name = "[{0}] {1}: {8}")
    @CsvFileSource(resources = "/testcases/medicine-service.csv", numLinesToSkip = 1)
    void executeMedicineServiceTests(String ruleId, String caseId, String action, Long inputPrice, Integer inputStock, String mockState, String expectedResult, ErrorCode expectedError, String note) {
        UUID medicineId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(medicineId);
        mockMedicine.setCurrentStock(10);
        mockMedicine.setUnitPriceVnd(150000L);
        mockMedicine.setIsActive(true);

        UUID categoryId = UUID.randomUUID();
        MedicineCreateRequest createReq = new MedicineCreateRequest("MED01", "Name", categoryId, "Unit", null, inputStock, inputPrice);
        MedicineUpdateRequest updateReq = new MedicineUpdateRequest("MED01", "New Name", categoryId, "Unit", null, inputStock, inputPrice);
        AddStockRequest addStockReq = new AddStockRequest(inputStock); // inputStock acts as quantityToAdd here

        MedicineCategory mockCategory = new MedicineCategory();
        mockCategory.setId(categoryId);

        // GIVEN
        switch (action) {
            case "CREATE":
                if ("VALID".equals(mockState) && inputPrice.compareTo(0L) >= 0 && inputStock >= 0) {
                    given(categoryRepository.findById(categoryId)).willReturn(Optional.of(mockCategory));
                    given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
                    given(medicineRepository.save(any(Medicine.class))).willAnswer(inv -> inv.getArgument(0));
                    given(medicineMapper.toMedicineResponse(mockMedicine)).willReturn(new MedicineResponse(medicineId, "MED01", "Name", null, null, "Unit", null, inputStock, inputPrice, true));
                }
                break;
            case "UPDATE":
                if ("VALID".equals(mockState) && inputPrice.compareTo(0L) >= 0 && inputStock >= 0) {
                    given(categoryRepository.findById(categoryId)).willReturn(Optional.of(mockCategory));
                    given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
                    given(medicineRepository.save(any(Medicine.class))).willAnswer(inv -> inv.getArgument(0));
                }
                break;
            case "ADD_STOCK":
                if ("VALID".equals(mockState) && inputStock > 0) {
                    given(medicineRepository.findByIdWithLock(medicineId)).willReturn(Optional.of(mockMedicine));
                    given(medicineRepository.save(any(Medicine.class))).willAnswer(inv -> inv.getArgument(0));
                }
                break;
            case "GET_MEDICINE":
            case "DELETE":
                if ("VALID".equals(mockState)) {
                    given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
                } else if ("NOT_FOUND".equals(mockState)) {
                    given(medicineRepository.findById(medicineId)).willReturn(Optional.empty());
                }
                break;
        }

        // WHEN & THEN
        if ("EXCEPTION".equals(expectedResult)) {
            assertThatThrownBy(() -> {
                switch (action) {
                    case "CREATE": medicineService.createMedicine(createReq); break;
                    case "UPDATE": medicineService.updateMedicine(medicineId, updateReq); break;
                    case "ADD_STOCK": medicineService.addStock(medicineId, addStockReq); break;
                    case "GET_MEDICINE": medicineService.getMedicine(medicineId); break;
                    case "DELETE": medicineService.deleteMedicine(medicineId); break;
                }
            }).isInstanceOf(BusinessException.class);
        } else {
            switch (action) {
                case "CREATE":
                    medicineService.createMedicine(createReq);
                    verify(medicineRepository).save(any(Medicine.class));
                    break;
                case "UPDATE":
                    medicineService.updateMedicine(medicineId, updateReq);
                    verify(medicineRepository).save(any(Medicine.class));
                    break;
                case "ADD_STOCK":
                    medicineService.addStock(medicineId, addStockReq);
                    verify(medicineRepository).save(any(Medicine.class));
                    assertThat(mockMedicine.getCurrentStock()).isEqualTo(10 + inputStock);
                    break;
                case "GET_MEDICINE":
                    medicineService.getMedicine(medicineId);
                    break;
                case "DELETE":
                    medicineService.deleteMedicine(medicineId);
                    assertThat(mockMedicine.getIsActive()).isFalse();
                    break;
            }
        }
    }

    @Test
    void should_searchMedicines() {
        Pageable pageable = PageRequest.of(0, 10);
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(UUID.randomUUID());
        Page<Medicine> mockPage = new PageImpl<>(List.of(mockMedicine));

        given(medicineRepository.findAll(any(Specification.class), eq(pageable))).willReturn(mockPage);
        given(medicineMapper.toMedicineResponse(any())).willReturn(new MedicineResponse(UUID.randomUUID(), "MED01", "Name", null, null, "Unit", null, 10, 150000L, true));

        var res = medicineService.searchMedicines("Test", UUID.randomUUID(), true, pageable);
        assertThat(res.data().content()).hasSize(1);
    }

    @Test
    void should_getAllMedicines() {
        Pageable pageable = PageRequest.of(0, 10);
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(UUID.randomUUID());
        Page<Medicine> mockPage = new PageImpl<>(List.of(mockMedicine));

        given(medicineRepository.findByCategoryId(any(UUID.class), eq(pageable))).willReturn(mockPage);
        given(medicineMapper.toMedicineResponse(any())).willReturn(new MedicineResponse(UUID.randomUUID(), "MED01", "Name", null, null, "Unit", null, 10, 150000L, true));

        var res = medicineService.getAllMedicines(UUID.randomUUID(), pageable);
        assertThat(res.data().content()).hasSize(1);
    }
    
    @Test
    void should_getAllMedicines_WithoutCategoryId() {
        Pageable pageable = PageRequest.of(0, 10);
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(UUID.randomUUID());
        Page<Medicine> mockPage = new PageImpl<>(List.of(mockMedicine));

        given(medicineRepository.findAll(eq(pageable))).willReturn(mockPage);
        given(medicineMapper.toMedicineResponse(any())).willReturn(new MedicineResponse(UUID.randomUUID(), "MED01", "Name", null, null, "Unit", null, 10, 150000L, true));

        var res = medicineService.getAllMedicines(null, pageable);
        assertThat(res.data().content()).hasSize(1);
    }

    @Test
    void should_listCategories() {
        MedicineCategory cat = new MedicineCategory();
        cat.setId(UUID.randomUUID());
        cat.setName("Test Category");
        cat.setIsActive(true);

        given(categoryRepository.findByIsActiveTrueOrderByNameAsc()).willReturn(List.of(cat));

        var res = medicineService.listCategories();
        assertThat(res).hasSize(1);
        assertThat(res.get(0).name()).isEqualTo("Test Category");
    }

    @Test
    void should_generateCode_whenNoCodeProvided() {
        UUID categoryId = UUID.randomUUID();
        MedicineCreateRequest createReq = new MedicineCreateRequest(null, "Aspirin", categoryId, "Unit", null, 10, 150000L);
        MedicineCategory mockCategory = new MedicineCategory();
        mockCategory.setId(categoryId);
        Medicine mockMedicine = new Medicine();

        given(categoryRepository.findById(categoryId)).willReturn(Optional.of(mockCategory));
        given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
        given(medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCase(any(), any())).willReturn(false);
        given(medicineRepository.existsByMedicineCodeIgnoreCase("MED-ASPIRIN-0001")).willReturn(true);
        given(medicineRepository.existsByMedicineCodeIgnoreCase("MED-ASPIRIN-0002")).willReturn(false);
        given(medicineRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        medicineService.createMedicine(createReq);

        verify(medicineRepository).save(any(Medicine.class));
        assertThat(mockMedicine.getMedicineCode()).isEqualTo("MED-ASPIRIN-0002");
    }
    
    @Test
    void should_throwException_whenCodeExists() {
        UUID categoryId = UUID.randomUUID();
        MedicineCreateRequest createReq = new MedicineCreateRequest("EXISTING_CODE", "Aspirin", categoryId, "Unit", null, 10, 150000L);
        MedicineCategory mockCategory = new MedicineCategory();
        mockCategory.setId(categoryId);
        Medicine mockMedicine = new Medicine();

        given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
        given(medicineRepository.existsByMedicineCodeIgnoreCase("EXISTING_CODE")).willReturn(true);

        assertThatThrownBy(() -> medicineService.createMedicine(createReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_005_MEDICINE_CODE_EXISTS);
    }
    
    @Test
    void should_throwException_whenNameExists() {
        UUID categoryId = UUID.randomUUID();
        MedicineCreateRequest createReq = new MedicineCreateRequest(null, "Aspirin", categoryId, "Unit", null, 10, 150000L);
        MedicineCategory mockCategory = new MedicineCategory();
        mockCategory.setId(categoryId);
        Medicine mockMedicine = new Medicine();

        given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
        given(medicineRepository.existsByMedicineCodeIgnoreCase("MED-ASPIRIN-0001")).willReturn(false);
        given(medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCase(any(), any())).willReturn(true);

        assertThatThrownBy(() -> medicineService.createMedicine(createReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_007_MEDICINE_NAME_EXISTS);
    }
    
    @Test
    void should_throwException_whenCategoryNotFound() {
        UUID categoryId = UUID.randomUUID();
        MedicineCreateRequest createReq = new MedicineCreateRequest(null, "Aspirin", categoryId, "Unit", null, 10, 150000L);
        MedicineCategory mockCategory = new MedicineCategory();
        mockCategory.setId(categoryId);
        Medicine mockMedicine = new Medicine();

        given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
        given(medicineRepository.existsByMedicineCodeIgnoreCase("MED-ASPIRIN-0001")).willReturn(false);
        given(medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCase(any(), any())).willReturn(false);
        given(categoryRepository.findById(categoryId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> medicineService.createMedicine(createReq))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
    }
    @Test
    void should_ThrowException_when_DeleteMedicineInUse() {
        UUID medicineId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        
        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
        given(prescriptionItemRepository.existsByMedicineId(medicineId)).willReturn(true);
        
        assertThatThrownBy(() -> medicineService.deleteMedicine(medicineId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_008_MEDICINE_IN_USE);
    }

    @Test
    void should_ThrowException_when_AddStockZeroOrNegative() {
        UUID medicineId = UUID.randomUUID();
        AddStockRequest req = new AddStockRequest(0);
        
        assertThatThrownBy(() -> medicineService.addStock(medicineId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_VALIDATION_FAILED);
    }

    @Test
    void should_ThrowException_when_UpdateMedicineCodeExists() {
        UUID medicineId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(medicineId);
        mockMedicine.setMedicineCode("OLD_CODE");
        
        MedicineUpdateRequest req = new MedicineUpdateRequest("NEW_CODE", "Name", categoryId, "Unit", null, 10, 150000L);
        
        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
        given(medicineRepository.existsByMedicineCodeIgnoreCaseAndIdNot("NEW_CODE", medicineId)).willReturn(true);
        
        assertThatThrownBy(() -> medicineService.updateMedicine(medicineId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_005_MEDICINE_CODE_EXISTS);
    }

    @Test
    void should_ThrowException_when_UpdateMedicineNameExists() {
        UUID medicineId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(medicineId);
        mockMedicine.setMedicineCode("OLD_CODE");
        
        MedicineUpdateRequest req = new MedicineUpdateRequest("OLD_CODE", "New Name", categoryId, "New Unit", null, 10, 150000L);
        
        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
        given(medicineRepository.existsByMedicineCodeIgnoreCaseAndIdNot("OLD_CODE", medicineId)).willReturn(false);
        given(medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCaseAndIdNot("New Name", "New Unit", medicineId)).willReturn(true);
        
        assertThatThrownBy(() -> medicineService.updateMedicine(medicineId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_007_MEDICINE_NAME_EXISTS);
    }

    @Test
    void should_ThrowException_when_UpdateMedicineCategoryNull() {
        UUID medicineId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(medicineId);
        mockMedicine.setMedicineCode("OLD_CODE");
        
        MedicineUpdateRequest req = new MedicineUpdateRequest("OLD_CODE", "Name", null, "Unit", null, 10, 150000L); // Category is null
        
        given(medicineRepository.findById(medicineId)).willReturn(Optional.of(mockMedicine));
        given(medicineRepository.existsByMedicineCodeIgnoreCaseAndIdNot("OLD_CODE", medicineId)).willReturn(false);
        given(medicineRepository.existsByNameIgnoreCaseAndUnitIgnoreCaseAndIdNot("Name", "Unit", medicineId)).willReturn(false);
        
        assertThatThrownBy(() -> medicineService.updateMedicine(medicineId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MED_006_MEDICINE_CATEGORY_NOT_FOUND);
    }

    @Test
    void should_searchMedicines_WithAllFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(UUID.randomUUID());
        Page<Medicine> mockPage = new PageImpl<>(List.of(mockMedicine));

        given(medicineRepository.findAll(any(Specification.class), eq(pageable))).willReturn(mockPage);
        given(medicineMapper.toMedicineResponse(any())).willReturn(new MedicineResponse(UUID.randomUUID(), "MED01", "Name", null, null, "Unit", null, 10, 150000L, true));

        var res = medicineService.searchMedicines("Test", UUID.randomUUID(), true, pageable);
        assertThat(res.data().content()).hasSize(1);
    }

    @Test
    void should_searchMedicines_WithNoFilters() {
        Pageable pageable = PageRequest.of(0, 10);
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(UUID.randomUUID());
        Page<Medicine> mockPage = new PageImpl<>(List.of(mockMedicine));

        given(medicineRepository.findAll((Specification<Medicine>) null, pageable)).willReturn(mockPage);
        given(medicineMapper.toMedicineResponse(any())).willReturn(new MedicineResponse(UUID.randomUUID(), "MED01", "Name", null, null, "Unit", null, 10, 150000L, true));

        var res = medicineService.searchMedicines(null, null, null, pageable);
        assertThat(res.data().content()).hasSize(1);
    }
}

