package com.astral.express.pccms.medicalrecord.service.impl;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicalrecord.dto.request.FinalizeMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.request.UpdateMedicalRecordRequest;
import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.mapper.MedicalRecordMapper;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicalrecord.event.MedicalRecordFinalizedEvent;
import com.astral.express.pccms.medicalrecord.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordMapper medicalRecordMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(UUID recordId, UpdateMedicalRecordRequest request) {
        // Find record
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_400_BAD_REQUEST)); // Or not found exception

        // Verify status
        if (record.getRecordStatus() != RecordStatus.DRAFT) {
            throw new BusinessException(ErrorCode.ERR_MR_006_RECORD_NOT_DRAFT);
        }

        // Validate vitals
        validateVitals(request.temperatureC(), request.heartRateBpm(), request.respiratoryRateBpm(),
                request.spo2Percent(), request.weightKg());

        // Update fields
        record.setTemperatureC(request.temperatureC());
        record.setHeartRateBpm(request.heartRateBpm());
        record.setRespiratoryRateBpm(request.respiratoryRateBpm());
        record.setWeightKg(request.weightKg());
        record.setBloodPressure(request.bloodPressure());
        record.setSpo2Percent(request.spo2Percent());
        record.setMucousMembraneColor(request.mucousMembraneColor());
        record.setCapillaryRefillSeconds(request.capillaryRefillSeconds());
        record.setPreliminaryDiagnosis(request.preliminaryDiagnosis());
        record.setTreatmentNote(request.treatmentNote());

        return medicalRecordMapper.toResponse(medicalRecordRepository.save(record));
    }

    @Override
    @Transactional
    public MedicalRecordResponse finalizeMedicalRecord(UUID recordId, FinalizeMedicalRecordRequest request) {
        // Find record
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_400_BAD_REQUEST)); // Or not found exception

        // Verify status
        if (record.getRecordStatus() != RecordStatus.DRAFT) {
            throw new BusinessException(ErrorCode.ERR_MR_006_RECORD_NOT_DRAFT);
        }

        // Verify final diagnosis
        if (request.finalDiagnosis() == null || request.finalDiagnosis().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.ERR_MR_007_MISSING_FINAL_DIAGNOSIS);
        }

        // Verify at least one vital sign exists
        if (!hasAtLeastOneVitalSign(record)) {
            throw new BusinessException(ErrorCode.ERR_MR_008_MISSING_VITAL_SIGNS);
        }

        // Update record
        record.setFinalDiagnosis(request.finalDiagnosis());
        record.setTreatmentNote(request.treatmentNote());
        record.setFollowUpAt(request.followUpAt());
        
        record.setRecordStatus(RecordStatus.FINALIZED);
        record.setLockedAt(OffsetDateTime.now());

        MedicalRecord savedRecord = medicalRecordRepository.save(record);
        log.info("Medical record {} finalized for pet {}", recordId, record.getPetId());

        eventPublisher.publishEvent(new MedicalRecordFinalizedEvent(
                savedRecord.getId(),
                savedRecord.getPetId(),
                savedRecord.getVetId()
        ));

        return medicalRecordMapper.toResponse(savedRecord);
    }

    private void validateVitals(BigDecimal temperature, Integer heartRate, Integer respiratoryRate, Integer spo2, BigDecimal weight) {
        if (temperature != null && (temperature.compareTo(new BigDecimal("30.0")) < 0 || temperature.compareTo(new BigDecimal("45.0")) > 0)) {
            throw new BusinessException(ErrorCode.ERR_MR_001_INVALID_TEMPERATURE);
        }
        if (heartRate != null && (heartRate < 40 || heartRate > 250)) {
            throw new BusinessException(ErrorCode.ERR_MR_002_INVALID_HEART_RATE);
        }
        if (respiratoryRate != null && (respiratoryRate < 10 || respiratoryRate > 100)) {
            throw new BusinessException(ErrorCode.ERR_MR_003_INVALID_RESPIRATORY_RATE);
        }
        if (spo2 != null && (spo2 < 70 || spo2 > 100)) {
            throw new BusinessException(ErrorCode.ERR_MR_004_INVALID_SPO2);
        }
        if (weight != null && (weight.compareTo(new BigDecimal("0.1")) < 0 || weight.compareTo(new BigDecimal("100.0")) > 0)) {
            throw new BusinessException(ErrorCode.ERR_MR_005_INVALID_WEIGHT);
        }
    }

    private boolean hasAtLeastOneVitalSign(MedicalRecord record) {
        return record.getTemperatureC() != null
                || record.getHeartRateBpm() != null
                || record.getRespiratoryRateBpm() != null
                || record.getSpo2Percent() != null
                || record.getWeightKg() != null
                || record.getBloodPressure() != null
                || record.getCapillaryRefillSeconds() != null
                || record.getMucousMembraneColor() != null;
    }
}
