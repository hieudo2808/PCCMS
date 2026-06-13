package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.appointment.dto.response.AppointmentResponse;
import com.astral.express.pccms.appointment.service.AppointmentLifecycleUseCase;
import com.astral.express.pccms.appointment.service.AppointmentQueryUseCase;
import com.astral.express.pccms.common.AbstractIntegrationTest;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

class MedicalRecordServiceIT extends AbstractIntegrationTest {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @MockitoBean
    private AppointmentLifecycleUseCase appointmentLifecycleUseCase;

    @MockitoBean
    private AppointmentQueryUseCase appointmentQueryUseCase;

    @MockitoBean
    private SecurityContextService securityContextService;

    private UUID testVetId;
    private UUID testPetId;
    private UUID testAppointmentId;

    @BeforeEach
    void setUp() {
        medicalRecordRepository.deleteAll();
        testVetId = UUID.randomUUID();
        testPetId = UUID.randomUUID();
        testAppointmentId = UUID.randomUUID();

        given(securityContextService.getCurrentUserId()).willReturn(testVetId);
        given(securityContextService.hasAnyRole("VETERINARIAN")).willReturn(true);
    }

    @Test
    void should_create_medical_record_and_persist_to_db() {
        // Arrange
        String code = "APT-12345";
        AppointmentResponse mockAppointment = new AppointmentResponse(
                testAppointmentId, code, null, null, null, null, null, null, testPetId, "Fluffy", testVetId, "Dr. Vet", null, null, null, null, null
        );
        given(appointmentLifecycleUseCase.startExam(eq(testAppointmentId), any())).willReturn(mockAppointment);

        // Act
        var response = medicalRecordService.getOrCreateMedicalRecordByAppointmentId(testAppointmentId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.recordCode()).isEqualTo("MR-" + code);
        
        MedicalRecord savedRecord = medicalRecordRepository.findById(response.id()).orElseThrow();
        assertThat(savedRecord.getPetId()).isEqualTo(testPetId);
        assertThat(savedRecord.getVetId()).isEqualTo(testVetId);
        assertThat(savedRecord.getAppointmentId()).isEqualTo(testAppointmentId);
        assertThat(savedRecord.getRecordStatus()).isEqualTo(RecordStatus.DRAFT);
    }

    @Test
    void should_update_medical_record_vitals_successfully() {
        // Arrange
        MedicalRecord record = MedicalRecord.builder()
                .recordCode("MR-TEST")
                .appointmentId(testAppointmentId)
                .petId(testPetId)
                .vetId(testVetId)
                .recordStatus(RecordStatus.DRAFT)
                .build();
        record = medicalRecordRepository.saveAndFlush(record);

        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                new BigDecimal("38.5"), 120, 30, new BigDecimal("5.0"), "120/80", 98, "Pink", new BigDecimal("1.5"), "Fever", "Give water"
        );

        // Act
        var response = medicalRecordService.updateMedicalRecord(record.getId(), request);

        // Assert
        assertThat(response).isNotNull();
        MedicalRecord updated = medicalRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(updated.getTemperatureC()).isEqualByComparingTo(new BigDecimal("38.5"));
        assertThat(updated.getHeartRateBpm()).isEqualTo(120);
    }

    @Test
    void should_throw_exception_when_updating_non_draft_record() {
        // Arrange
        MedicalRecord record = MedicalRecord.builder()
                .recordCode("MR-TEST-FINAL")
                .appointmentId(testAppointmentId)
                .petId(testPetId)
                .vetId(testVetId)
                .recordStatus(RecordStatus.FINALIZED)
                .build();
        record = medicalRecordRepository.saveAndFlush(record);

        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                new BigDecimal("38.5"), null, null, null, null, null, null, null, null, null
        );

        // Act & Assert
        UUID id = record.getId();
        assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(id, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ERR_MR_006_RECORD_NOT_DRAFT);
    }

    @Test
    void should_throw_DataIntegrityViolationException_on_duplicate_appointment() {
        // Arrange
        MedicalRecord record1 = MedicalRecord.builder()
                .recordCode("MR-DUP-1")
                .appointmentId(testAppointmentId)
                .petId(testPetId)
                .vetId(testVetId)
                .recordStatus(RecordStatus.DRAFT)
                .build();
        medicalRecordRepository.saveAndFlush(record1);

        MedicalRecord record2 = MedicalRecord.builder()
                .recordCode("MR-DUP-2")
                .appointmentId(testAppointmentId) // Same appointment ID
                .petId(testPetId)
                .vetId(testVetId)
                .recordStatus(RecordStatus.DRAFT)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> medicalRecordRepository.saveAndFlush(record2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void should_finalize_medical_record_successfully() {
        // Arrange
        MedicalRecord record = MedicalRecord.builder()
                .recordCode("MR-FINALIZE")
                .appointmentId(testAppointmentId)
                .petId(testPetId)
                .vetId(testVetId)
                .temperatureC(new BigDecimal("38.5")) // Required vital sign
                .recordStatus(RecordStatus.DRAFT)
                .build();
        record = medicalRecordRepository.saveAndFlush(record);

        FinalizeMedicalRecordRequest request = new FinalizeMedicalRecordRequest(
                "Healthy", OffsetDateTime.now().plusDays(7), "No treatment needed"
        );

        // Act
        var response = medicalRecordService.finalizeMedicalRecord(record.getId(), request);

        // Assert
        assertThat(response.recordStatus()).isEqualTo(RecordStatus.FINALIZED);
        MedicalRecord finalized = medicalRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(finalized.getRecordStatus()).isEqualTo(RecordStatus.FINALIZED);
        assertThat(finalized.getFinalDiagnosis()).isEqualTo("Healthy");
    }
}
