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

    @Column(name = "temperature_c", precision = 5, scale = 2)
    private BigDecimal temperatureC;

    @Column(name = "heart_rate_bpm")
    private Integer heartRateBpm;

    @Column(name = "respiratory_rate_bpm")
    private Integer respiratoryRateBpm;

    @Column(name = "weight_kg", precision = 7, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "blood_pressure", length = 40)
    private String bloodPressure;

    @Column(name = "spo2_percent")
    private Integer spo2Percent;

    @Column(name = "mucous_membrane_color", length = 80)
    private String mucousMembraneColor;

    @Column(name = "capillary_refill_seconds", precision = 5, scale = 2)
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
}
