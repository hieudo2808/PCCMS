package com.astral.express.pccms.medicalrecord.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import java.util.Optional;
import com.astral.express.pccms.pet.dto.response.PetResponse;
import com.astral.express.pccms.medicalrecord.entity.Prescription;
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
    @Mock
    private org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock
    private com.astral.express.pccms.appointment.service.AppointmentLifecycleUseCase appointmentLifecycleUseCase;
    @Mock
    private com.astral.express.pccms.appointment.service.AppointmentQueryUseCase appointmentQueryUseCase;
    @Mock
    private com.astral.express.pccms.identity.security.SecurityContextService SecurityContextService;

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
        com.astral.express.pccms.medicine.entity.Medicine medicine = new com.astral.express.pccms.medicine.entity.Medicine();
        medicine.setId(item.getMedicineId());
        medicine.setName("Paracetamol");
        medicine.setUnit("Viên");
        when(medicineRepository.findAllById(List.of(item.getMedicineId()))).thenReturn(List.of(medicine));

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

    @Test
    void should_UpdateMedicalRecord_Success() {
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest request = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest.class);
            
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setId(recordId);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(java.util.Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.updateMedicalRecord(recordId, request);
        
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(record);
    }

    @Test
    void should_ThrowException_when_UpdateMedicalRecordNotFound() {
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest request = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest.class);
            
        when(medicalRecordRepository.findById(recordId)).thenReturn(java.util.Optional.empty());
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(recordId, request))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", com.astral.express.pccms.common.exception.ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_FinalizeMedicalRecord_Success() {
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest request = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest.class);
        org.mockito.Mockito.when(request.finalDiagnosis()).thenReturn("Diagnosis");
            
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setId(recordId);
        record.setAppointmentId(UUID.randomUUID());
        record.setPetId(UUID.randomUUID());
        record.setVetId(UUID.randomUUID());
        record.setTemperatureC(new java.math.BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(java.util.Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.finalizeMedicalRecord(recordId, request);
        
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(record);
        verify(appointmentLifecycleUseCase).completeMedicalAppointment(any(), any());
        verify(eventPublisher).publishEvent(any(com.astral.express.pccms.medicalrecord.event.MedicalRecordFinalizedEvent.class));
    }

    @Test
    void should_ThrowException_when_FinalizeMedicalRecordSaveFails() {
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setRecordStatus(com.astral.express.pccms.medicalrecord.entity.RecordStatus.DRAFT);
        record.setTemperatureC(new java.math.BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        org.mockito.Mockito.lenient().when(medicalRecordRepository.findById(recordId)).thenReturn(java.util.Optional.of(record));
        org.mockito.Mockito.lenient().when(medicalRecordRepository.save(any())).thenReturn(record);
        org.mockito.Mockito.lenient().when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.empty()); // simulate save fail
        
        com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest req = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest.class);
        org.mockito.Mockito.when(req.finalDiagnosis()).thenReturn("Diagnosis");
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> medicalRecordService.finalizeMedicalRecord(recordId, req))
                .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.astral.express.pccms.common.exception.ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_ThrowException_when_GetOrCreateMedicalRecordSaveFails() {
        UUID appointmentId = UUID.randomUUID();
        com.astral.express.pccms.appointment.dto.response.AppointmentResponse apptResponse = org.mockito.Mockito.mock(com.astral.express.pccms.appointment.dto.response.AppointmentResponse.class);
        
        org.mockito.Mockito.lenient().when(SecurityContextService.hasAnyRole("VETERINARIAN")).thenReturn(true);
        org.mockito.Mockito.lenient().when(SecurityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(appointmentLifecycleUseCase.startExam(any(), any())).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(java.util.Optional.empty());
        
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setId(UUID.randomUUID());
        when(medicalRecordRepository.save(any())).thenReturn(record);
        when(medicalRecordRepository.findResponseById(any())).thenReturn(java.util.Optional.empty()); // simulate save fail
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId))
                .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", com.astral.express.pccms.common.exception.ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_GetMedicalRecordById_Success() {
        UUID recordId = UUID.randomUUID();
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getMedicalRecordById(recordId);
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }

    @Test
    void should_ThrowException_when_GetMedicalRecordByIdNotFound() {
        UUID recordId = UUID.randomUUID();
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.empty());
        
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> medicalRecordService.getMedicalRecordById(recordId))
            .isInstanceOf(com.astral.express.pccms.common.exception.BusinessException.class);
    }

    @Test
    void should_GetOrCreateMedicalRecordByAppointmentId_Exist() {
        UUID appointmentId = UUID.randomUUID();
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        com.astral.express.pccms.appointment.dto.response.AppointmentResponse apptResponse = org.mockito.Mockito.mock(com.astral.express.pccms.appointment.dto.response.AppointmentResponse.class);
        
        org.mockito.Mockito.lenient().when(SecurityContextService.hasAnyRole("VETERINARIAN")).thenReturn(false);
        when(appointmentQueryUseCase.getAppointmentById(appointmentId)).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }

    @Test
    void should_GetOrCreateMedicalRecordByAppointmentId_CreateNew() {
        UUID appointmentId = UUID.randomUUID();
        com.astral.express.pccms.appointment.dto.response.AppointmentResponse apptResponse = org.mockito.Mockito.mock(com.astral.express.pccms.appointment.dto.response.AppointmentResponse.class);
        
        org.mockito.Mockito.lenient().when(SecurityContextService.hasAnyRole("VETERINARIAN")).thenReturn(true);
        org.mockito.Mockito.lenient().when(SecurityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(appointmentLifecycleUseCase.startExam(any(), any())).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(java.util.Optional.empty());
        
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setId(UUID.randomUUID());
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(record.getId(), null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(record.getId())).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(any());
    }

    @Test
    void getOrCreateMedicalRecordByAppointmentId_whenSecurityContextServiceNull() {
        MedicalRecordService serviceWithNullSecurity = new MedicalRecordService(
                medicalRecordRepository,
                eventPublisher,
                appointmentLifecycleUseCase,
                appointmentQueryUseCase,
                null, // SecurityContextService is null
                petService,
                prescriptionRepository,
                medicineRepository
        );
        
        UUID appointmentId = UUID.randomUUID();
        AppointmentResponse appointmentResponse = org.mockito.Mockito.mock(AppointmentResponse.class);
        org.mockito.Mockito.lenient().when(appointmentResponse.appointmentCode()).thenReturn("CODE");
        org.mockito.Mockito.lenient().when(appointmentResponse.petId()).thenReturn(UUID.randomUUID());
        org.mockito.Mockito.lenient().when(appointmentResponse.assignedVetId()).thenReturn(UUID.randomUUID());
        when(appointmentQueryUseCase.getAppointmentById(appointmentId)).thenReturn(appointmentResponse);
        
        com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse response = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse.class);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(Optional.of(response));
        
        com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse result = serviceWithNullSecurity.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        assertThat(result).isNotNull();
    }

    @Test
    void getOwnerMedicalRecords_whenMedicineNotFound() {
        UUID petId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        PetResponse pet = org.mockito.Mockito.mock(PetResponse.class);
        org.mockito.Mockito.when(pet.ownerId()).thenReturn(currentUserId);
        when(petService.getPet(petId)).thenReturn(pet);
        
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse record = new com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse(
                recordId, "MR-CODE", petId, "Vet", null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(medicalRecordRepository.findOwnerResponsesByPetIdAndStatus(petId, RecordStatus.FINALIZED)).thenReturn(List.of(record));
        
        Prescription p = new Prescription();
        p.setId(UUID.randomUUID());
        p.setMedicalRecordId(recordId);
        com.astral.express.pccms.medicalrecord.entity.PrescriptionItem item = new com.astral.express.pccms.medicalrecord.entity.PrescriptionItem();
        item.setId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID()); // This ID will not be found in medicineRepository
        p.setItems(List.of(item));
        when(prescriptionRepository.findByMedicalRecordIdIn(List.of(recordId))).thenReturn(List.of(p));
        
        when(medicineRepository.findAllById(any())).thenReturn(List.of()); // Returns empty, so medicine is null
        
        List<com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse> result = medicalRecordService.getOwnerMedicalRecords(petId, currentUserId);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).prescription().items().get(0).medicineName()).isNull();
        assertThat(result.get(0).prescription().items().get(0).medicineUnit()).isNull();
    }

    @Test
    void finalizeMedicalRecord_whenSecurityContextServiceNull() {
        MedicalRecordService serviceWithNullSecurity = new MedicalRecordService(
                medicalRecordRepository,
                eventPublisher,
                appointmentLifecycleUseCase,
                appointmentQueryUseCase,
                null, // SecurityContextService is null
                petService,
                prescriptionRepository,
                medicineRepository
        );
        
        UUID recordId = UUID.randomUUID();
        com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest request = org.mockito.Mockito.mock(com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest.class);
        org.mockito.Mockito.when(request.finalDiagnosis()).thenReturn("Diagnosis");
            
        com.astral.express.pccms.medicalrecord.entity.MedicalRecord record = new com.astral.express.pccms.medicalrecord.entity.MedicalRecord();
        record.setId(recordId);
        record.setAppointmentId(UUID.randomUUID());
        record.setPetId(UUID.randomUUID());
        record.setVetId(UUID.randomUUID());
        record.setTemperatureC(new java.math.BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(java.util.Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = org.mockito.Mockito.mock(MedicalRecordResponse.class);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(java.util.Optional.of(mockResponse));
        
        MedicalRecordResponse result = serviceWithNullSecurity.finalizeMedicalRecord(recordId, request);
        org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    }


}
