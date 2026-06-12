package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.Prescription;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicalrecord.repository.PrescriptionRepository;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @InjectMocks
    private PrescriptionService prescriptionService;

    @Captor
    private ArgumentCaptor<Prescription> prescriptionCaptor;

    @Captor
    private ArgumentCaptor<Medicine> medicineCaptor;

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/prescription-creation.csv", numLinesToSkip = 1)
    void should_HandlePrescriptionCreation_BasedOnStock(
            String ruleId, String caseId, String action, Integer inputQuantity, Integer mockStock,
            String expectedResult, String expectedErrorCode, String note) {

        // GIVEN
        UUID recordId = UUID.randomUUID();
        UUID medicineId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();

        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setRecordStatus(RecordStatus.DRAFT);
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        Medicine medicine = new Medicine();
        medicine.setId(medicineId);
        medicine.setCurrentStock(mockStock);
        medicine.setUnitPriceVnd(50000L);
        medicine.setIsActive(true);
        given(medicineRepository.findByIdWithLock(medicineId)).willReturn(Optional.of(medicine));

        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                vetId,
                "Note",
                List.of(new PrescriptionItemRequest(medicineId, "1 per day", inputQuantity, "After meal"))
        );

        if ("SUCCESS".equals(expectedResult)) {
            given(prescriptionRepository.save(any(Prescription.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            prescriptionService.createPrescription(recordId, request);

            // THEN
            verify(prescriptionRepository).save(prescriptionCaptor.capture());
            Prescription savedPrescription = prescriptionCaptor.getValue();
            assertThat(savedPrescription.getMedicalRecordId()).isEqualTo(recordId);
            assertThat(savedPrescription.getVetId()).isEqualTo(vetId);
            assertThat(savedPrescription.getItems()).hasSize(1);
            assertThat(savedPrescription.getItems().get(0).getQuantity()).isEqualTo(inputQuantity);
            assertThat(savedPrescription.getItems().get(0).getUnitPriceVnd()).isEqualTo(50000L);

            verify(medicineRepository).save(medicineCaptor.capture());
            Medicine updatedMedicine = medicineCaptor.getValue();
            assertThat(updatedMedicine.getCurrentStock()).isEqualTo(mockStock - inputQuantity);

        } else if ("EXCEPTION".equals(expectedResult)) {
            // WHEN & THEN
            assertThatThrownBy(() -> prescriptionService.createPrescription(recordId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode.errorCode")
                    .isEqualTo(expectedErrorCode);
        }
    }
}

