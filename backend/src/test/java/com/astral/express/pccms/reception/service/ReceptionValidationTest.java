package com.astral.express.pccms.reception.service;

import com.astral.express.pccms.common.exception.BusinessException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReceptionValidationTest {

    @ParameterizedTest(name = "{0} - {1}")
    @CsvFileSource(resources = "/testcases/uc013-appointment-reception.csv", numLinesToSkip = 1)
    void should_validate_quick_appointment_when_csv_cases_are_executed(
            String ruleId,
            String caseId,
            String phone,
            String ownerName,
            String petName,
            String symptom,
            String expectedResult) {
        // GIVEN
        boolean valid = "VALID".equals(expectedResult);

        // WHEN & THEN
        if (valid) {
            assertThatCode(() -> ReceptionValidation.validateQuickAppointment(phone, ownerName, petName, symptom))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment(phone, ownerName, petName, symptom))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvFileSource(resources = "/testcases/uc015-boarding-care-log.csv", numLinesToSkip = 1)
    void should_validate_boarding_care_log_when_csv_cases_are_executed(
            String ruleId,
            String caseId,
            int logDateOffsetDays,
            String periodCode,
            String feedingStatus,
            String hygieneStatus,
            String expectedResult) {
        // GIVEN
        LocalDate logDate = LocalDate.now().plusDays(logDateOffsetDays);
        boolean valid = "VALID".equals(expectedResult);

        // WHEN & THEN
        if (valid) {
            assertThatCode(() -> ReceptionValidation.validateCareLog(logDate, periodCode, feedingStatus, hygieneStatus))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ReceptionValidation.validateCareLog(logDate, periodCode, feedingStatus, hygieneStatus))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvFileSource(resources = "/testcases/uc015-care-log-media.csv", numLinesToSkip = 1)
    void should_validate_care_log_media_when_csv_cases_are_executed(
            String ruleId,
            String caseId,
            long sizeBytes,
            String mimeType,
            String expectedResult) {
        // GIVEN
        boolean valid = "VALID".equals(expectedResult);

        // WHEN & THEN
        if (valid) {
            assertThatCode(() -> ReceptionValidation.validateCareLogMedia(sizeBytes, mimeType))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ReceptionValidation.validateCareLogMedia(sizeBytes, mimeType))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @ParameterizedTest(name = "{0} - {1}")
    @CsvFileSource(resources = "/testcases/uc016-grooming-status.csv", numLinesToSkip = 1)
    void should_validate_grooming_status_transition_when_csv_cases_are_executed(
            String ruleId,
            String caseId,
            String currentStatus,
            String nextStatus,
            String expectedResult) {
        // GIVEN
        boolean valid = "VALID".equals(expectedResult);

        // WHEN & THEN
        if (valid) {
            assertThatCode(() -> ReceptionValidation.validateGroomingTransition(currentStatus, nextStatus))
                    .doesNotThrowAnyException();
        } else {
            assertThatThrownBy(() -> ReceptionValidation.validateGroomingTransition(currentStatus, nextStatus))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @org.junit.jupiter.api.Test
    void validateQuickAppointment_nulls() {
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment(null, "A", "B", "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("", "A", "B", "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", null, "B", "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", "", "B", "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", "A", null, "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", "A", "", "C"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", "A", "B", null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateQuickAppointment("012345678", "A", "B", ""))
                .isInstanceOf(BusinessException.class);
    }

    @org.junit.jupiter.api.Test
    void validateCareLog_nulls() {
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(null, "MORNING", "A", "B"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(LocalDate.now(), null, "A", "B"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(LocalDate.now(), "MORNING", null, "B"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(LocalDate.now(), "MORNING", "", "B"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(LocalDate.now(), "MORNING", "A", null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ReceptionValidation.validateCareLog(LocalDate.now(), "MORNING", "A", ""))
                .isInstanceOf(BusinessException.class);
    }

    @org.junit.jupiter.api.Test
    void validateCareLogMedia_nulls() {
        assertThatThrownBy(() -> ReceptionValidation.validateCareLogMedia(100, null))
                .isInstanceOf(BusinessException.class);
    }

    @org.junit.jupiter.api.Test
    void validateGroomingTransition_nulls() {
        assertThatThrownBy(() -> ReceptionValidation.validateGroomingTransition("PENDING", null))
                .isInstanceOf(BusinessException.class);
    }

}