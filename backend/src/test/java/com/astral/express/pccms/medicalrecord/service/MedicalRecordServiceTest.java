package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.mapper.MedicalRecordMapper;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicalrecord.event.MedicalRecordFinalizedEvent;
import com.astral.express.pccms.medicalrecord.service.impl.MedicalRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private MedicalRecordMapper medicalRecordMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    @Captor
    private ArgumentCaptor<MedicalRecordFinalizedEvent> eventCaptor;

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/medical-record-validation.csv", numLinesToSkip = 1)
    void should_ValidateVitalSigns_when_UpdatingMedicalRecord(
            String ruleId, String caseId, Double temperature, Integer heartRate, Integer respiratoryRate,
            Integer spo2, Double weight, String expectedResult, String errorCode) {

        // GIVEN
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setRecordStatus(RecordStatus.DRAFT);

        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                temperature != null ? BigDecimal.valueOf(temperature) : null,
                heartRate,
                respiratoryRate,
                weight != null ? BigDecimal.valueOf(weight) : null,
                "120/80",
                spo2,
                "Pink",
                BigDecimal.valueOf(1.5),
                "Fever",
                "Rest"
        );

        if ("VALID".equals(expectedResult)) {
            MedicalRecordResponse mockResponse = new MedicalRecordResponse(
                    recordId, "MR-123", null, null, null, RecordStatus.DRAFT,
                    request.temperatureC(), request.heartRateBpm(), request.respiratoryRateBpm(),
                    request.weightKg(), request.bloodPressure(), request.spo2Percent(),
                    request.mucousMembraneColor(), request.capillaryRefillSeconds(),
                    request.preliminaryDiagnosis(), null, request.treatmentNote(),
                    null, null, null, null
            );
            given(medicalRecordRepository.save(any(MedicalRecord.class))).willReturn(record);
            given(medicalRecordMapper.toResponse(any(MedicalRecord.class))).willReturn(mockResponse);

            // WHEN
            MedicalRecordResponse response = medicalRecordService.updateMedicalRecord(recordId, request);

            // THEN
            assertThat(response).isNotNull();
        } else {
            // WHEN & THEN
            assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(recordId, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode.errorCode")
                    .isEqualTo(errorCode);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/medical-record-lifecycle.csv", numLinesToSkip = 1)
    void should_EnforceLifecycleRules_when_UpdatingOrFinalizing(
            String ruleId, String caseId, String action, String currentStatus,
            Boolean hasFinalDiagnosis, Boolean hasVitalSigns, String expectedResult, String errorCode) {

        // GIVEN
        UUID recordId = UUID.randomUUID();
        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setRecordStatus(RecordStatus.valueOf(currentStatus));

        if (hasVitalSigns) {
            record.setTemperatureC(BigDecimal.valueOf(38.0));
        }

        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));

        if ("UPDATE".equals(action)) {
            UpdateMedicalRecordRequest request = new UpdateMedicalRecordRequest(
                    BigDecimal.valueOf(38.0), 120, 30, BigDecimal.valueOf(5.0),
                    "120/80", 98, "Pink", BigDecimal.valueOf(1.5),
                    "Fever", "Rest"
            );

            if ("VALID".equals(expectedResult)) {
                given(medicalRecordRepository.save(any(MedicalRecord.class))).willReturn(record);
                given(medicalRecordMapper.toResponse(any(MedicalRecord.class))).willReturn(new MedicalRecordResponse(
                        recordId, "MR-123", null, null, null, RecordStatus.DRAFT,
                        request.temperatureC(), request.heartRateBpm(), request.respiratoryRateBpm(),
                        request.weightKg(), request.bloodPressure(), request.spo2Percent(),
                        request.mucousMembraneColor(), request.capillaryRefillSeconds(),
                        request.preliminaryDiagnosis(), null, request.treatmentNote(),
                        null, null, null, null
                ));

                // WHEN
                MedicalRecordResponse response = medicalRecordService.updateMedicalRecord(recordId, request);

                // THEN
                assertThat(response).isNotNull();
            } else {
                // WHEN & THEN
                assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(recordId, request))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode.errorCode")
                        .isEqualTo(errorCode);
            }
        } else if ("FINALIZE".equals(action)) {
            FinalizeMedicalRecordRequest request = new FinalizeMedicalRecordRequest(
                    hasFinalDiagnosis ? "Infection" : "",
                    OffsetDateTime.now().plusDays(7),
                    "Antibiotics"
            );

            if ("VALID".equals(expectedResult)) {
                given(medicalRecordRepository.save(any(MedicalRecord.class))).willReturn(record);
                given(medicalRecordMapper.toResponse(any(MedicalRecord.class))).willReturn(new MedicalRecordResponse(
                        recordId, "MR-123", null, null, null, RecordStatus.FINALIZED,
                        record.getTemperatureC(), null, null, null, null, null, null, null,
                        record.getPreliminaryDiagnosis(), request.finalDiagnosis(), request.treatmentNote(),
                        request.followUpAt(), OffsetDateTime.now(), null, null
                ));

                // WHEN
                MedicalRecordResponse response = medicalRecordService.finalizeMedicalRecord(recordId, request);

                // THEN
                assertThat(response).isNotNull();
                assertThat(record.getRecordStatus()).isEqualTo(RecordStatus.FINALIZED);
                assertThat(record.getLockedAt()).isNotNull();
            } else {
                // WHEN & THEN
                assertThatThrownBy(() -> medicalRecordService.finalizeMedicalRecord(recordId, request))
                        .isInstanceOf(BusinessException.class)
                        .extracting("errorCode.errorCode")
                        .isEqualTo(errorCode);
            }
        }
    }

    /**
     * BR-05: When a medical record is finalized, a MedicalRecordFinalizedEvent
     * MUST be published via ApplicationEventPublisher (loose coupling).
     */
    @Test
    void should_PublishMedicalRecordFinalizedEvent_when_RecordIsFinalized() {
        // GIVEN
        UUID recordId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        UUID vetId = UUID.randomUUID();

        MedicalRecord record = new MedicalRecord();
        record.setId(recordId);
        record.setPetId(petId);
        record.setVetId(vetId);
        record.setRecordStatus(RecordStatus.DRAFT);
        record.setTemperatureC(BigDecimal.valueOf(38.5)); // At least one vital sign required

        given(medicalRecordRepository.findById(recordId)).willReturn(Optional.of(record));
        given(medicalRecordRepository.save(any(MedicalRecord.class))).willReturn(record);
        given(medicalRecordMapper.toResponse(any(MedicalRecord.class))).willReturn(
                new MedicalRecordResponse(recordId, "MR-001", petId, vetId, null,
                        RecordStatus.FINALIZED, null, null, null, null, null, null, null, null,
                        null, "Final diagnosis", null, null, OffsetDateTime.now(), null, null)
        );

        FinalizeMedicalRecordRequest request = new FinalizeMedicalRecordRequest(
                "Final diagnosis", OffsetDateTime.now().plusDays(7), "Antibiotics"
        );

        // WHEN
        medicalRecordService.finalizeMedicalRecord(recordId, request);

        // THEN (BR-05)
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        MedicalRecordFinalizedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.recordId()).isEqualTo(recordId);
        assertThat(publishedEvent.petId()).isEqualTo(petId);
        assertThat(publishedEvent.vetId()).isEqualTo(vetId);
    }
}
