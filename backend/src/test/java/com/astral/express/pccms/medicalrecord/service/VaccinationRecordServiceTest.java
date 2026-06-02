package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.medicalrecord.dto.request.CreateVaccinationRequest;
import com.astral.express.pccms.medicalrecord.entity.VaccinationRecord;
import com.astral.express.pccms.medicalrecord.repository.VaccinationRecordRepository;
import com.astral.express.pccms.medicalrecord.service.impl.VaccinationRecordServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VaccinationRecordServiceTest {

    @Mock
    private VaccinationRecordRepository vaccinationRecordRepository;

    @InjectMocks
    private VaccinationRecordServiceImpl vaccinationRecordService;

    @Captor
    private ArgumentCaptor<VaccinationRecord> captor;

    @ParameterizedTest
    @CsvFileSource(resources = "/testcases/vaccination-validation.csv", numLinesToSkip = 1)
    void should_ValidateVaccinationRecord(
            String ruleId, String caseId, String action, String vaccinationDateStr, String nextDueDateStr,
            String expectedResult, String expectedErrorCode, String note) {

        // GIVEN
        UUID petId = UUID.randomUUID();
        LocalDate vaccinationDate = LocalDate.parse(vaccinationDateStr);
        LocalDate nextDueDate = nextDueDateStr != null && !nextDueDateStr.isEmpty() ? LocalDate.parse(nextDueDateStr) : null;

        CreateVaccinationRequest request = new CreateVaccinationRequest(
                petId,
                null,
                "Rabies",
                vaccinationDate,
                nextDueDate,
                "Note",
                UUID.randomUUID()
        );

        if ("SUCCESS".equals(expectedResult)) {
            // WHEN
            vaccinationRecordService.createVaccinationRecord(request);

            // THEN
            verify(vaccinationRecordRepository).save(captor.capture());
            VaccinationRecord savedRecord = captor.getValue();
            assertThat(savedRecord.getPetId()).isEqualTo(petId);
            assertThat(savedRecord.getVaccinationDate()).isEqualTo(vaccinationDate);
            assertThat(savedRecord.getNextDueDate()).isEqualTo(nextDueDate);
        } else if ("EXCEPTION".equals(expectedResult)) {
            // WHEN & THEN
            assertThatThrownBy(() -> vaccinationRecordService.createVaccinationRecord(request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode.errorCode")
                    .isEqualTo(expectedErrorCode);
        }
    }
}
