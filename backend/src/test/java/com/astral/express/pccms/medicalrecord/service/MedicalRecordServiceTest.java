package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import com.astral.express.pccms.pet.service.PetService;
import com.astral.express.pccms.medicalrecord.repository.PrescriptionRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PetService petService;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private com.astral.express.pccms.medicine.repository.MedicineRepository medicineRepository;

    @InjectMocks
    private MedicalRecordService medicalRecordService;

    @Test
    void should_FetchResponsesByVetId_when_VetIdIsProvided() {
        // GIVEN
        UUID vetId = UUID.randomUUID();
        MedicalRecordResponse mockResponse1 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", vetId, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        MedicalRecordResponse mockResponse2 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Bella", vetId, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);

        when(medicalRecordRepository.findResponsesByVetId(vetId)).thenReturn(List.of(mockResponse1, mockResponse2));

        // WHEN
        List<MedicalRecordResponse> results = medicalRecordService.getMedicalRecords(vetId);

        // THEN
        assertThat(results).hasSize(2);
        assertThat(results.get(0).petName()).isEqualTo("Rex");
        assertThat(results.get(1).petName()).isEqualTo("Bella");
        verify(medicalRecordRepository).findResponsesByVetId(vetId);
    }

    @Test
    void should_FetchAllResponses_when_VetIdIsNull() {
        // GIVEN
        MedicalRecordResponse mockResponse1 = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", null, "Dr. Smith", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        when(medicalRecordRepository.findAllResponses()).thenReturn(List.of(mockResponse1));

        // WHEN
        List<MedicalRecordResponse> results = medicalRecordService.getMedicalRecords(null);

        // THEN
        assertThat(results).hasSize(1);
        verify(medicalRecordRepository).findAllResponses();
    }

    @Test
    void should_ThrowForbiddenException_when_UserIsNotOwner() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();
        
        com.astral.express.pccms.pet.dto.response.PetResponse petResponse = new com.astral.express.pccms.pet.dto.response.PetResponse(
            petId, otherOwnerId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(petService.getPet(petId)).thenReturn(petResponse);

        // WHEN & THEN
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> medicalRecordService.getOwnerMedicalRecords(petId, currentUserId))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", com.astral.express.pccms.common.exception.ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ReturnRecordsWithPrescriptions_when_OwnerRequestsRecords() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        com.astral.express.pccms.pet.dto.response.PetResponse petResponse = new com.astral.express.pccms.pet.dto.response.PetResponse(
            petId, currentUserId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(petService.getPet(petId)).thenReturn(petResponse);

        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse ownerResponse = new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse(
                recordId,
                "MR-123",
                petId,
                "Vet",
                BigDecimal.valueOf(38.5),
                BigDecimal.valueOf(5.0),
                100, // heartRateBpm
                20, // respiratoryRateBpm
                "120/80", // bloodPressure
                98, // spo2Percent
                "Hồng hào", // mucousMembraneColor
                BigDecimal.valueOf(2.0), // capillaryRefillSeconds
                "Healthy",
                "None",
                null,
                java.time.OffsetDateTime.now(),
                null
        );
        when(medicalRecordRepository.findOwnerResponsesByPetIdAndStatus(petId, com.astral.express.pccms.medicalrecord.entity.RecordStatus.FINALIZED))
            .thenReturn(List.of(ownerResponse));

        com.astral.express.pccms.medicalrecord.entity.PrescriptionItem item = new com.astral.express.pccms.medicalrecord.entity.PrescriptionItem();
        item.setId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID());
        item.setQuantity(10);
        
        com.astral.express.pccms.medicalrecord.entity.Prescription prescription = new com.astral.express.pccms.medicalrecord.entity.Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setMedicalRecordId(recordId);
        prescription.setItems(List.of(item));
        
        when(prescriptionRepository.findByMedicalRecordIdIn(List.of(recordId))).thenReturn(List.of(prescription));
        when(medicineRepository.findAllById(List.of(item.getMedicineId()))).thenReturn(List.of());

        // WHEN
        List<com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse> results = medicalRecordService.getOwnerMedicalRecords(petId, currentUserId);

        // THEN
        assertThat(results).hasSize(1);
        
        com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse response = results.get(0);
        assertThat(response.prescription()).isNotNull();
        assertThat(response.prescription().items()).hasSize(1);
        
        // Assert Vitals Mapping
        assertThat(response.temperatureC()).isEqualByComparingTo("38.5");
        assertThat(response.weightKg()).isEqualByComparingTo("5.0");
        assertThat(response.heartRateBpm()).isEqualTo(100);
        assertThat(response.respiratoryRateBpm()).isEqualTo(20);
        assertThat(response.bloodPressure()).isEqualTo("120/80");
        assertThat(response.spo2Percent()).isEqualTo(98);
        assertThat(response.mucousMembraneColor()).isEqualTo("Hồng hào");
        assertThat(response.capillaryRefillSeconds()).isEqualByComparingTo("2.0");
    }
}


