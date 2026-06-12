package com.astral.express.pccms.medicalrecord.repository;

import com.astral.express.pccms.medicalrecord.dto.response.MedicalRecordOwnerResponse;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.transaction.annotation.Transactional;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@Transactional
class MedicalRecordRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    private UUID petId;

    @BeforeEach
    void setUp() {
        petId = UUID.randomUUID();

        MedicalRecord record1 = MedicalRecord.builder()
                .recordCode("MR-001")
                .petId(petId)
                .vetId(UUID.randomUUID())
                .recordStatus(RecordStatus.FINALIZED)
                .temperatureC(java.math.BigDecimal.valueOf(38.5))
                .weightKg(java.math.BigDecimal.valueOf(5.2))
                .heartRateBpm(120)
                .respiratoryRateBpm(30)
                .bloodPressure("120/80")
                .spo2Percent(98)
                .mucousMembraneColor("Hồng")
                .capillaryRefillSeconds(java.math.BigDecimal.valueOf(2.0))
                .build();
        medicalRecordRepository.save(record1);

        MedicalRecord record2 = MedicalRecord.builder()
                .recordCode("MR-002")
                .petId(petId)
                .vetId(UUID.randomUUID())
                .recordStatus(RecordStatus.DRAFT)
                .build();
        medicalRecordRepository.save(record2);

        MedicalRecord record3 = MedicalRecord.builder()
                .recordCode("MR-003")
                .petId(UUID.randomUUID()) // Different pet
                .vetId(UUID.randomUUID())
                .recordStatus(RecordStatus.FINALIZED)
                .build();
        medicalRecordRepository.save(record3);
    }

    @Test
    void should_ReturnOnlyFinalizedRecordsForSpecificPet() {
        // WHEN
        List<MedicalRecordOwnerResponse> results = medicalRecordRepository.findOwnerResponsesByPetIdAndStatus(petId, RecordStatus.FINALIZED);

        // THEN
        assertThat(results).hasSize(1);
        assertThat(results.get(0).recordCode()).isEqualTo("MR-001");
        assertThat(results.get(0).petId()).isEqualTo(petId);
        
        // Assert Vitals
        MedicalRecordOwnerResponse response = results.get(0);
        assertThat(response.temperatureC()).isEqualByComparingTo("38.5");
        assertThat(response.weightKg()).isEqualByComparingTo("5.2");
        assertThat(response.heartRateBpm()).isEqualTo(120);
        assertThat(response.respiratoryRateBpm()).isEqualTo(30);
        assertThat(response.bloodPressure()).isEqualTo("120/80");
        assertThat(response.spo2Percent()).isEqualTo(98);
        assertThat(response.mucousMembraneColor()).isEqualTo("Hồng");
        assertThat(response.capillaryRefillSeconds()).isEqualByComparingTo("2.0");
    }
}
