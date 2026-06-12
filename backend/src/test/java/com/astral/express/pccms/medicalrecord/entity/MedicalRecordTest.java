package com.astral.express.pccms.medicalrecord.entity;

import com.astral.express.pccms.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MedicalRecordTest {

    private MedicalRecord record;

    @BeforeEach
    void setUp() {
        record = new MedicalRecord();
        record.setId(UUID.randomUUID());
        record.setRecordStatus(RecordStatus.DRAFT);
    }

    @Test
    void should_UpdateVitals_when_DraftStatusAndValidData() {
        record.updateVitals(
                BigDecimal.valueOf(38.5),
                120,
                30,
                98,
                BigDecimal.valueOf(10.5),
                "120/80",
                "Pink",
                BigDecimal.valueOf(1.5),
                "Looks healthy",
                "Routine checkup"
        );

        assertThat(record.getTemperatureC()).isEqualTo(BigDecimal.valueOf(38.5));
        assertThat(record.getHeartRateBpm()).isEqualTo(120);
        assertThat(record.getRespiratoryRateBpm()).isEqualTo(30);
        assertThat(record.getSpo2Percent()).isEqualTo(98);
        assertThat(record.getWeightKg()).isEqualTo(BigDecimal.valueOf(10.5));
        assertThat(record.getBloodPressure()).isEqualTo("120/80");
        assertThat(record.getMucousMembraneColor()).isEqualTo("Pink");
        assertThat(record.getCapillaryRefillSeconds()).isEqualTo(BigDecimal.valueOf(1.5));
        assertThat(record.getPreliminaryDiagnosis()).isEqualTo("Looks healthy");
        assertThat(record.getTreatmentNote()).isEqualTo("Routine checkup");
    }

    @Test
    void should_ThrowException_when_UpdateVitalsAndNotDraft() {
        record.setRecordStatus(RecordStatus.FINALIZED);

        assertThatThrownBy(() -> record.updateVitals(
                BigDecimal.valueOf(38.5), null, null, null, null, null, null, null, null, null
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void should_ThrowException_when_InvalidTemperature() {
        assertThatThrownBy(() -> record.updateVitals(
                BigDecimal.valueOf(-1), null, null, null, null, null, null, null, null, null
        )).isInstanceOf(BusinessException.class);
    }

    @Test
    void should_FinalizeRecord_when_ValidData() {
        record.setTemperatureC(BigDecimal.valueOf(38.5)); // At least one vital sign

        OffsetDateTime now = OffsetDateTime.now();
        record.finalizeRecord("Healthy", "None", now);

        assertThat(record.getRecordStatus()).isEqualTo(RecordStatus.FINALIZED);
        assertThat(record.getFinalDiagnosis()).isEqualTo("Healthy");
        assertThat(record.getTreatmentNote()).isEqualTo("None");
        assertThat(record.getFollowUpAt()).isEqualTo(now);
        assertThat(record.getLockedAt()).isNotNull();
    }

    @Test
    void should_ThrowException_when_FinalizingWithoutVitals() {
        assertThatThrownBy(() -> record.finalizeRecord("Healthy", "None", OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void should_ThrowException_when_FinalizingWithoutDiagnosis() {
        record.setTemperatureC(BigDecimal.valueOf(38.5));

        assertThatThrownBy(() -> record.finalizeRecord("", "None", OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class);
    }
}
