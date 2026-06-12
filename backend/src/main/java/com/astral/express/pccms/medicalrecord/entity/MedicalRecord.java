package com.astral.express.pccms.medicalrecord.entity;

import com.astral.express.pccms.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "medical_records")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalRecord extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "record_code", nullable = false, unique = true, length = 60)
    private String recordCode;

    @Column(name = "appointment_id", unique = true)
    private UUID appointmentId;

    @Column(name = "pet_id", nullable = false)
    private UUID petId;

    @Column(name = "vet_id", nullable = false)
    private UUID vetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_status", nullable = false, length = 30)
    @Builder.Default
    private RecordStatus recordStatus = RecordStatus.DRAFT;

    @Column(name = "temperature_c")
    private BigDecimal temperatureC;

    @Column(name = "heart_rate_bpm")
    private Integer heartRateBpm;

    @Column(name = "respiratory_rate_bpm")
    private Integer respiratoryRateBpm;

    @Column(name = "weight_kg")
    private BigDecimal weightKg;

    @Column(name = "blood_pressure", length = 40)
    private String bloodPressure;

    @Column(name = "spo2_percent")
    private Integer spo2Percent;

    @Column(name = "mucous_membrane_color", length = 80)
    private String mucousMembraneColor;

    @Column(name = "capillary_refill_seconds")
    private BigDecimal capillaryRefillSeconds;

    @Column(name = "preliminary_diagnosis", columnDefinition = "TEXT")
    private String preliminaryDiagnosis;

    @Column(name = "final_diagnosis", columnDefinition = "TEXT")
    private String finalDiagnosis;

    @Column(name = "treatment_note", columnDefinition = "TEXT")
    private String treatmentNote;

    @Column(name = "follow_up_at")
    private OffsetDateTime followUpAt;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    public void updateVitals(BigDecimal temperatureC, Integer heartRateBpm, Integer respiratoryRateBpm,
                             Integer spo2Percent, BigDecimal weightKg, String bloodPressure,
                             String mucousMembraneColor, BigDecimal capillaryRefillSeconds,
                             String preliminaryDiagnosis, String treatmentNote) {
        if (this.recordStatus != RecordStatus.DRAFT) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_006_RECORD_NOT_DRAFT);
        }
        
        if (temperatureC != null && temperatureC.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_001_INVALID_TEMPERATURE);
        }
        if (heartRateBpm != null && heartRateBpm < 0) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_002_INVALID_HEART_RATE);
        }
        if (respiratoryRateBpm != null && respiratoryRateBpm < 0) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_003_INVALID_RESPIRATORY_RATE);
        }
        if (spo2Percent != null && (spo2Percent < 0 || spo2Percent > 100)) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_004_INVALID_SPO2);
        }
        if (weightKg != null && weightKg.compareTo(BigDecimal.ZERO) < 0) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_005_INVALID_WEIGHT);
        }

        this.temperatureC = temperatureC;
        this.heartRateBpm = heartRateBpm;
        this.respiratoryRateBpm = respiratoryRateBpm;
        this.spo2Percent = spo2Percent;
        this.weightKg = weightKg;
        this.bloodPressure = bloodPressure;
        this.mucousMembraneColor = mucousMembraneColor;
        this.capillaryRefillSeconds = capillaryRefillSeconds;
        this.preliminaryDiagnosis = preliminaryDiagnosis;
        this.treatmentNote = treatmentNote;
    }

    public void finalizeRecord(String finalDiagnosis, String treatmentNote, OffsetDateTime followUpAt) {
        if (this.recordStatus != RecordStatus.DRAFT) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_006_RECORD_NOT_DRAFT);
        }
        if (finalDiagnosis == null || finalDiagnosis.trim().isEmpty()) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_007_MISSING_FINAL_DIAGNOSIS);
        }
        if (!hasAtLeastOneVitalSign()) {
            throw new com.astral.express.pccms.common.exception.BusinessException(com.astral.express.pccms.common.exception.ErrorCode.ERR_MR_008_MISSING_VITAL_SIGNS);
        }

        this.finalDiagnosis = finalDiagnosis;
        this.treatmentNote = treatmentNote;
        this.followUpAt = followUpAt;
        this.recordStatus = RecordStatus.FINALIZED;
        this.lockedAt = OffsetDateTime.now();
    }

    private boolean hasAtLeastOneVitalSign() {
        return this.temperatureC != null
                || this.heartRateBpm != null
                || this.respiratoryRateBpm != null
                || this.spo2Percent != null
                || this.weightKg != null
                || this.bloodPressure != null
                || this.capillaryRefillSeconds != null
                || this.mucousMembraneColor != null;
    }
}
