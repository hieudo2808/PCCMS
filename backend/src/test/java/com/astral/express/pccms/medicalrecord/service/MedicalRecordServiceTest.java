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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.astral.express.pccms.appointment.service.AppointmentLifecycleUseCase;
import com.astral.express.pccms.appointment.service.AppointmentQueryUseCase;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.PrescriptionItem;
import com.astral.express.pccms.medicalrecord.event.MedicalRecordFinalizedEvent;
import com.astral.express.pccms.medicine.entity.Medicine;
import java.time.OffsetDateTime;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private PetService petService;

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicineRepository medicineRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private AppointmentLifecycleUseCase appointmentLifecycleUseCase;
    @Mock
    private AppointmentQueryUseCase appointmentQueryUseCase;
    @Mock
    private SecurityContextService securityContextService;

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
        
        PetResponse petResponse = new PetResponse(
            petId, otherOwnerId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(petService.getPet(petId)).thenReturn(petResponse);

        // WHEN & THEN
        assertThatThrownBy(() -> medicalRecordService.getOwnerMedicalRecords(petId, currentUserId))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_403_FORBIDDEN);
    }

    @Test
    void should_ReturnRecordsWithPrescriptions_when_OwnerRequestsRecords() {
        // GIVEN
        UUID petId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        
        PetResponse petResponse = new PetResponse(
            petId, currentUserId, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(petService.getPet(petId)).thenReturn(petResponse);

        UUID recordId = UUID.randomUUID();
        MedicalRecordOwnerResponse ownerResponse = new MedicalRecordOwnerResponse(
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
                OffsetDateTime.now(),
                null
        );
        when(medicalRecordRepository.findOwnerResponsesByPetIdAndStatus(petId, RecordStatus.FINALIZED))
            .thenReturn(List.of(ownerResponse));

        PrescriptionItem item = new PrescriptionItem();
        item.setId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID());
        item.setQuantity(10);
        
        Prescription prescription = new Prescription();
        prescription.setId(UUID.randomUUID());
        prescription.setMedicalRecordId(recordId);
        prescription.setItems(List.of(item));
        
        when(prescriptionRepository.findByMedicalRecordIdIn(List.of(recordId))).thenReturn(List.of(prescription));
        Medicine medicine = new Medicine();
        medicine.setId(item.getMedicineId());
        medicine.setName("Paracetamol");
        medicine.setUnit("Viên");
        when(medicineRepository.findAllById(List.of(item.getMedicineId()))).thenReturn(List.of(medicine));

        // WHEN
        List<MedicalRecordOwnerResponse> results = medicalRecordService.getOwnerMedicalRecords(petId, currentUserId);

        // THEN
        assertThat(results).hasSize(1);
        
        MedicalRecordOwnerResponse response = results.get(0);
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
        UpdateMedicalRecordRequest request = Mockito.mock(UpdateMedicalRecordRequest.class);
            
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.updateMedicalRecord(recordId, request);
        
        assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(record);
    }

    @Test
    void should_ThrowException_when_UpdateMedicalRecordNotFound() {
        UUID recordId = UUID.randomUUID();
        UpdateMedicalRecordRequest request = Mockito.mock(UpdateMedicalRecordRequest.class);
            
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(recordId, request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_FinalizeMedicalRecord_Success() {
        UUID recordId = UUID.randomUUID();
        FinalizeMedicalRecordRequest request = Mockito.mock(FinalizeMedicalRecordRequest.class);
        Mockito.when(request.finalDiagnosis()).thenReturn("Diagnosis");
            
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setAppointmentId(UUID.randomUUID());
        record.setPetId(UUID.randomUUID());
        record.setVetId(UUID.randomUUID());
        record.setTemperatureC(new BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.finalizeMedicalRecord(recordId, request);
        
        assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(record);
        verify(appointmentLifecycleUseCase).completeMedicalAppointment(any(), any());
        verify(eventPublisher).publishEvent(any(MedicalRecordFinalizedEvent.class));
    }

    @Test
    void should_ThrowException_when_FinalizeMedicalRecordSaveFails() {
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setRecordStatus(RecordStatus.DRAFT);
        record.setTemperatureC(new BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        Mockito.lenient().when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        Mockito.lenient().when(medicalRecordRepository.save(any())).thenReturn(record);
        Mockito.lenient().when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.empty()); // simulate save fail
        
        FinalizeMedicalRecordRequest req = Mockito.mock(FinalizeMedicalRecordRequest.class);
        Mockito.when(req.finalDiagnosis()).thenReturn("Diagnosis");
        
        assertThatThrownBy(() -> medicalRecordService.finalizeMedicalRecord(recordId, req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_ThrowException_when_GetOrCreateMedicalRecordSaveFails() {
        UUID appointmentId = UUID.randomUUID();
        AppointmentResponse apptResponse = Mockito.mock(AppointmentResponse.class);
        
        Mockito.lenient().when(securityContextService.hasAnyRole("VETERINARIAN")).thenReturn(true);
        Mockito.lenient().when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(appointmentLifecycleUseCase.startExam(any(), any())).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        
        MedicalRecord record = new MedicalRecord();
        record.setId(UUID.randomUUID());
        when(medicalRecordRepository.save(any())).thenReturn(record);
        when(medicalRecordRepository.findResponseById(any())).thenReturn(Optional.empty()); // simulate save fail
        
        assertThatThrownBy(() -> medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_400_BAD_REQUEST);
    }

    @Test
    void should_GetMedicalRecordById_Success() {
        UUID recordId = UUID.randomUUID();
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(recordId, null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getMedicalRecordById(recordId);
        assertThat(result).isNotNull();
    }

    @Test
    void should_ThrowException_when_GetMedicalRecordByIdNotFound() {
        UUID recordId = UUID.randomUUID();
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> medicalRecordService.getMedicalRecordById(recordId))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_GetOrCreateMedicalRecordByAppointmentId_Exist() {
        UUID appointmentId = UUID.randomUUID();
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(UUID.randomUUID(), null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        
        AppointmentResponse apptResponse = Mockito.mock(AppointmentResponse.class);
        
        Mockito.lenient().when(securityContextService.hasAnyRole("VETERINARIAN")).thenReturn(false);
        when(appointmentQueryUseCase.getAppointmentById(appointmentId)).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        assertThat(result).isNotNull();
    }

    @Test
    void should_GetOrCreateMedicalRecordByAppointmentId_CreateNew() {
        UUID appointmentId = UUID.randomUUID();
        AppointmentResponse apptResponse = Mockito.mock(AppointmentResponse.class);
        
        Mockito.lenient().when(securityContextService.hasAnyRole("VETERINARIAN")).thenReturn(true);
        Mockito.lenient().when(securityContextService.getCurrentUserId()).thenReturn(UUID.randomUUID());
        when(appointmentLifecycleUseCase.startExam(any(), any())).thenReturn(apptResponse);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(Optional.empty());
        
        MedicalRecord record = new MedicalRecord();
        record.setId(UUID.randomUUID());
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = new MedicalRecordResponse(record.getId(), null, null, null, "Rex", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        when(medicalRecordRepository.findResponseById(record.getId())).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        assertThat(result).isNotNull();
        verify(medicalRecordRepository).save(any());
    }

    @Test
    void getOrCreateMedicalRecordByAppointmentId_whensecurityContextServiceNull() {
        MedicalRecordService serviceWithNullSecurity = new MedicalRecordService(
                medicalRecordRepository,
                eventPublisher,
                appointmentLifecycleUseCase,
                appointmentQueryUseCase,
                null, // securityContextService is null
                petService,
                prescriptionRepository,
                medicineRepository
        );
        
        UUID appointmentId = UUID.randomUUID();
        AppointmentResponse appointmentResponse = Mockito.mock(AppointmentResponse.class);
        Mockito.lenient().when(appointmentResponse.appointmentCode()).thenReturn("CODE");
        Mockito.lenient().when(appointmentResponse.petId()).thenReturn(UUID.randomUUID());
        Mockito.lenient().when(appointmentResponse.assignedVetId()).thenReturn(UUID.randomUUID());
        when(appointmentQueryUseCase.getAppointmentById(appointmentId)).thenReturn(appointmentResponse);
        
        MedicalRecordResponse response = Mockito.mock(MedicalRecordResponse.class);
        when(medicalRecordRepository.findResponseByAppointmentId(appointmentId)).thenReturn(Optional.of(response));
        
        MedicalRecordResponse result = serviceWithNullSecurity.getOrCreateMedicalRecordByAppointmentId(appointmentId);
        assertThat(result).isNotNull();
    }

    @Test
    void getOwnerMedicalRecords_whenMedicineNotFound() {
        UUID petId = UUID.randomUUID();
        UUID currentUserId = UUID.randomUUID();
        PetResponse pet = Mockito.mock(PetResponse.class);
        Mockito.when(pet.ownerId()).thenReturn(currentUserId);
        when(petService.getPet(petId)).thenReturn(pet);
        
        UUID recordId = UUID.randomUUID();
        MedicalRecordOwnerResponse record = new MedicalRecordOwnerResponse(
                recordId, "MR-CODE", petId, "Vet", null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(medicalRecordRepository.findOwnerResponsesByPetIdAndStatus(petId, RecordStatus.FINALIZED)).thenReturn(List.of(record));
        
        Prescription p = new Prescription();
        p.setId(UUID.randomUUID());
        p.setMedicalRecordId(recordId);
        PrescriptionItem item = new PrescriptionItem();
        item.setId(UUID.randomUUID());
        item.setMedicineId(UUID.randomUUID()); // This ID will not be found in medicineRepository
        p.setItems(List.of(item));
        when(prescriptionRepository.findByMedicalRecordIdIn(List.of(recordId))).thenReturn(List.of(p));
        
        when(medicineRepository.findAllById(any())).thenReturn(List.of()); // Returns empty, so medicine is null
        
        List<MedicalRecordOwnerResponse> result = medicalRecordService.getOwnerMedicalRecords(petId, currentUserId);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).prescription().items().get(0).medicineName()).isNull();
        assertThat(result.get(0).prescription().items().get(0).medicineUnit()).isNull();
    }

    @Test
    void finalizeMedicalRecord_whensecurityContextServiceNull() {
        MedicalRecordService serviceWithNullSecurity = new MedicalRecordService(
                medicalRecordRepository,
                eventPublisher,
                appointmentLifecycleUseCase,
                appointmentQueryUseCase,
                null, // securityContextService is null
                petService,
                prescriptionRepository,
                medicineRepository
        );
        
        UUID recordId = UUID.randomUUID();
        FinalizeMedicalRecordRequest request = Mockito.mock(FinalizeMedicalRecordRequest.class);
        Mockito.when(request.finalDiagnosis()).thenReturn("Diagnosis");
            
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setAppointmentId(UUID.randomUUID());
        record.setPetId(UUID.randomUUID());
        record.setVetId(UUID.randomUUID());
        record.setTemperatureC(new BigDecimal("38.5"));
        record.setHeartRateBpm(100);
        record.setRespiratoryRateBpm(20);
        
        when(medicalRecordRepository.findById(recordId)).thenReturn(Optional.of(record));
        when(medicalRecordRepository.save(any())).thenReturn(record);
        
        MedicalRecordResponse mockResponse = Mockito.mock(MedicalRecordResponse.class);
        when(medicalRecordRepository.findResponseById(recordId)).thenReturn(Optional.of(mockResponse));
        
        MedicalRecordResponse result = serviceWithNullSecurity.finalizeMedicalRecord(recordId, request);
        assertThat(result).isNotNull();
    }


}
