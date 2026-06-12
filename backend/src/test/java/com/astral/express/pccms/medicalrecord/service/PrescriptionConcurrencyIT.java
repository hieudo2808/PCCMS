package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.medicalrecord.dto.request.CreatePrescriptionRequest;
import com.astral.express.pccms.medicalrecord.dto.request.PrescriptionItemRequest;
import com.astral.express.pccms.medicalrecord.entity.MedicalRecord;
import com.astral.express.pccms.medicalrecord.entity.RecordStatus;
import com.astral.express.pccms.medicalrecord.repository.MedicalRecordRepository;
import com.astral.express.pccms.medicine.entity.Medicine;
import com.astral.express.pccms.medicine.entity.MedicineCategory;
import com.astral.express.pccms.medicine.repository.MedicineCategoryRepository;
import com.astral.express.pccms.medicine.repository.MedicineRepository;
import com.astral.express.pccms.user.entity.Roles;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.entity.UserStatus;
import com.astral.express.pccms.user.repository.RoleRepository;
import com.astral.express.pccms.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class PrescriptionConcurrencyIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:15-alpine"))
            .withDatabaseName("pccms_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    private PrescriptionService prescriptionService;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private MedicineCategoryRepository medicineCategoryRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    private UUID vetId;
    private UUID medicalRecordId;
    private UUID medicineId;

    @BeforeEach
    void setUp() {
        medicineRepository.deleteAll();
        medicineCategoryRepository.deleteAll();
        medicalRecordRepository.deleteAll();
        userRepository.deleteAll();

        // Prepare Vet
        Roles vetRole = new Roles();
        vetRole.setCode("VETERINARIAN");
        vetRole = roleRepository.save(vetRole);
        
        Users vet = Users.builder()
                .fullName("Vet 1")
                .email("vet@test.com")
                .passwordHash("hash")
                .role(vetRole)
                .statusCode(UserStatus.ACTIVE)
                .build();
        vet = userRepository.save(vet);
        vetId = vet.getId();

        // Prepare Medical Record
        MedicalRecord record = new MedicalRecord();
        record.setRecordCode("REC-001");
        record.setVetId(vetId);
        record.setPetId(UUID.randomUUID());
        record.setRecordStatus(RecordStatus.DRAFT);
        record = medicalRecordRepository.save(record);
        medicalRecordId = record.getId();

        // Prepare Medicine
        MedicineCategory category = new MedicineCategory();
        category.setName("Antibiotics");
        category = medicineCategoryRepository.save(category);

        Medicine medicine = new Medicine();
        medicine.setMedicineCode("M001");
        medicine.setName("Amoxicillin");
        medicine.setCategory(category);
        medicine.setUnit("Box");
        medicine.setCurrentStock(10); // Stock is 10
        medicine.setUnitPriceVnd(100000L);
        medicine.setIsActive(true);
        medicine = medicineRepository.save(medicine);
        medicineId = medicine.getId();
    }

    @Test
    void should_PreventDataRace_when_TwoThreadsCreatePrescriptionConcurrently() throws InterruptedException {
        // GIVEN: Stock is 10. Thread 1 wants 8, Thread 2 wants 8.
        // Total requested is 16 > 10. One should succeed, one should fail.
        
        int threadCount = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        // WHEN
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                            vetId,
                            "Take daily",
                            List.of(new PrescriptionItemRequest(medicineId, "1 per day", 8, null))
                    );

                    readyLatch.countDown();
                    startLatch.await(); // Wait for all threads to be ready

                    prescriptionService.createPrescription(medicalRecordId, request);
                    successCount.incrementAndGet();
                } catch (BusinessException e) {
                    if ("ERR_MED_002_INSUFFICIENT_STOCK".equals(e.getErrorCode().getErrorCode())) {
                        failureCount.incrementAndGet();
                    } else {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    // Might throw OptimisticLockException or CannotAcquireLockException
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown(); // Start all threads simultaneously
        doneLatch.await(5, TimeUnit.SECONDS);

        // THEN
        // One should succeed, one should fail
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);

        // The final stock should be 10 - 8 = 2
        Medicine updatedMedicine = medicineRepository.findById(medicineId).orElseThrow();
        assertThat(updatedMedicine.getCurrentStock()).isEqualTo(2);
        
        executorService.shutdown();
    }
}
