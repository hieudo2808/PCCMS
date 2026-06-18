package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.response.PrescriptionResponse;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.Prescription;
import com.astral.express.pccms.medicalrecord.entity.PrescriptionItem;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import org.junit.jupiter.api.Test;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicineRepository medicineRepository;

    @Mock
    private PrescriptionRepository prescriptionRepository;
    @Mock
    private SecurityContextService securityContextService;

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

    @Test
    void should_ResolveVetId_FromSecurityContext() {
        UUID recordId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        
        given(securityContextService.getCurrentUserId()).willReturn(currentUserId);
        
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(null, "Note", List.of());
        
        given(prescriptionRepository.save(any(Prescription.class))).willAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse response = prescriptionService.createPrescription(recordId, request);
        assertThat(response.vetId()).isEqualTo(currentUserId);
    }

    @Test
    void should_ResolveVetId_FromRecord() {
        UUID recordId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setVetId(vetId);
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        
        given(securityContextService.getCurrentUserId()).willReturn(null);
        
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(null, "Note", List.of());
        
        given(prescriptionRepository.save(any(Prescription.class))).willAnswer(invocation -> invocation.getArgument(0));

        PrescriptionResponse response = prescriptionService.createPrescription(recordId, request);
        assertThat(response.vetId()).isEqualTo(vetId);
    }

    @Test
    void should_ThrowException_when_ResolveVetIdFails() {
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setVetId(null);
        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        
        given(securityContextService.getCurrentUserId()).willReturn(null);
        
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(null, "Note", List.of());
        
        assertThatThrownBy(() -> prescriptionService.createPrescription(recordId, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_401_UNAUTHORIZED);
    }

    @Test
    void should_ThrowException_when_ListPrescriptionsRecordNotFound() {
        UUID recordId = UUID.randomUUID();
        given(medicalRecordRepository.existsById(recordId)).willReturn(false);
        
        assertThatThrownBy(() -> prescriptionService.listPrescriptions(recordId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_ListPrescriptions_Success() {
        UUID recordId = UUID.randomUUID();
        given(medicalRecordRepository.existsById(recordId)).willReturn(true);
        
        Prescription prescription = new Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setMedicalRecordId(recordId);
        prescription.setItems(List.of());
        
        given(prescriptionRepository.findByMedicalRecordIdOrderByIssuedAtDesc(recordId))
                .willReturn(List.of(prescription));
                
        List<PrescriptionResponse> responses = prescriptionService.listPrescriptions(recordId);
        assertThat(responses).hasSize(1);
    }

    @Test
    void should_ListPrescriptions_BatchLoadMedicinesForItems() {
        UUID recordId = UUID.randomUUID();
        UUID medicineId1 = UUID.randomUUID();
        UUID medicineId2 = UUID.randomUUID();
        UUID medicineId3 = UUID.randomUUID();
        given(medicalRecordRepository.existsById(recordId)).willReturn(true);

        PrescriptionItem item1 = new PrescriptionItem();
        item1.setMedicineId(medicineId1);
        item1.setDosage("1 tablet");
        item1.setQuantity(1);
        item1.setInstruction("After meal");
        item1.setUnitPriceVnd(1000L);

        PrescriptionItem item2 = new PrescriptionItem();
        item2.setMedicineId(medicineId2);
        item2.setDosage("2 ml");
        item2.setQuantity(2);
        item2.setInstruction("Morning");
        item2.setUnitPriceVnd(2000L);

        PrescriptionItem item3 = new PrescriptionItem();
        item3.setMedicineId(medicineId3);
        item3.setDosage("1 capsule");
        item3.setQuantity(3);
        item3.setInstruction("Evening");
        item3.setUnitPriceVnd(3000L);

        Prescription prescription1 = new Prescription();
        prescription1.setId(UUID.randomUUID());
        prescription1.setMedicalRecordId(recordId);
        prescription1.setItems(List.of(item1, item2));

        Prescription prescription2 = new Prescription();
        prescription2.setId(UUID.randomUUID());
        prescription2.setMedicalRecordId(recordId);
        prescription2.setItems(List.of(item3));

        Medicine medicine1 = new Medicine();
        medicine1.setId(medicineId1);
        medicine1.setName("Medicine A");
        medicine1.setUnit("tablet");

        Medicine medicine2 = new Medicine();
        medicine2.setId(medicineId2);
        medicine2.setName("Medicine B");
        medicine2.setUnit("ml");

        Medicine medicine3 = new Medicine();
        medicine3.setId(medicineId3);
        medicine3.setName("Medicine C");
        medicine3.setUnit("capsule");

        given(prescriptionRepository.findByMedicalRecordIdOrderByIssuedAtDesc(recordId))
                .willReturn(List.of(prescription1, prescription2));
        given(medicineRepository.findAllById(List.of(medicineId1, medicineId2, medicineId3)))
                .willReturn(List.of(medicine1, medicine2, medicine3));

        List<PrescriptionResponse> responses = prescriptionService.listPrescriptions(recordId);

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).items()).hasSize(2);
        assertThat(responses.get(1).items()).hasSize(1);
        assertThat(responses.get(0).items().get(0).medicineName()).isEqualTo("Medicine A");
        assertThat(responses.get(0).items().get(1).medicineName()).isEqualTo("Medicine B");
        assertThat(responses.get(1).items().get(0).medicineName()).isEqualTo("Medicine C");
        verify(medicineRepository).findAllById(List.of(medicineId1, medicineId2, medicineId3));
        verify(medicineRepository, never()).findById(any(UUID.class));
    }
}

