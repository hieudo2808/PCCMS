package com.astral.express.pccms.medicine.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicine.dto.request.AddStockRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineCreateRequest;
import com.astral.express.pccms.medicine.dto.request.MedicineUpdateRequest;
import com.astral.express.pccms.medicine.dto.response.MedicineResponse;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.mapper.MedicineMapper;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.medicine.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicineServiceTest {

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private MedicineCategoryRepository categoryRepository;

    @Mock
    private MedicineMapper medicineMapper;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    @ParameterizedTest(name = "[{0}] {1}: {8}")
    @CsvFileSource(resources = "/testcases/medicine-service.csv", numLinesToSkip = 1)
    void executeMedicineServiceTests(String ruleId, String caseId, String action, BigDecimal inputPrice, Integer inputStock, String mockState, String expectedResult, ErrorCode expectedError, String note) {
        UUID medicineId = UUID.randomUUID();
        Medicine mockMedicine = new Medicine();
        mockMedicine.setId(medicineId);
        mockMedicine.setCurrentStock(10);
        mockMedicine.setUnitPriceVnd(BigDecimal.valueOf(150000));
        mockMedicine.setIsActive(true);

        MedicineCreateRequest createReq = new MedicineCreateRequest("MED01", "Name", null, "Unit", null, inputStock, inputPrice);
        MedicineUpdateRequest updateReq = new MedicineUpdateRequest("New Name", null, "Unit", null, inputStock, inputPrice);
        AddStockRequest addStockReq = new AddStockRequest(inputStock); // inputStock acts as quantityToAdd here

        // GIVEN
        switch (action) {
            case "CREATE":
                if ("VALID".equals(mockState) && inputPrice.compareTo(BigDecimal.ZERO) >= 0 && inputStock >= 0) {
                    given(medicineMapper.toMedicine(createReq)).willReturn(mockMedicine);
                    given(medicineRepository.save(any(Medicine.class))).willAnswer(inv -> inv.getArgument(0));
                    given(medicineMapper.toMedicineResponse(mockMedicine)).willReturn(new MedicineResponse(medicineId, "MED01", "Name", null, null, "Unit", null, inputStock, inputPrice, true));
                }
                break;
            case "UPDATE":
                if ("VALID".equals(mockState) && inputPrice.compareTo(BigDecimal.ZERO) >= 0 && inputStock >= 0) {
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
}
