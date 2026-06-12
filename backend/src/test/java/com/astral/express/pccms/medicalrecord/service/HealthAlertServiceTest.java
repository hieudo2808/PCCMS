package com.astral.express.pccms.medicalrecord.service;

import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.medicalrecord.dto.request.CreateHealthAlertRequest;
import com.astral.express.pccms.medicalrecord.entity.AlertSeverity;
import com.astral.express.pccms.medicalrecord.entity.HealthAlert;
import com.astral.express.pccms.medicalrecord.repository.HealthAlertRepository;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class HealthAlertServiceTest {

    @Mock
    private HealthAlertRepository healthAlertRepository;

    @InjectMocks
    private HealthAlertService healthAlertService;

    @Captor
    private ArgumentCaptor<HealthAlert> captor;

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvFileSource(resources = "/testcases/health-alert-testcases.csv", numLinesToSkip = 1)
    void should_ProcessHealthAlert(String ruleId, String caseId, String action, String expectedError) {
        if ("CREATE".equals(action)) {
            UUID petId = UUID.randomUUID();
            CreateHealthAlertRequest request = new CreateHealthAlertRequest(
                    petId, null, AlertSeverity.HIGH, "Allergic to amoxicillin", UUID.randomUUID()
            );

            healthAlertService.createHealthAlert(request);

            verify(healthAlertRepository).save(captor.capture());
            HealthAlert savedAlert = captor.getValue();
            assertThat(savedAlert.getPetId()).isEqualTo(petId);
            assertThat(savedAlert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
            assertThat(savedAlert.getMessage()).isEqualTo("Allergic to amoxicillin");
        } else if ("RESOLVE".equals(action)) {
            UUID alertId = UUID.randomUUID();
            UUID resolvedBy = UUID.randomUUID();
            HealthAlert alert = new HealthAlert();
            alert.setId(alertId);

            given(healthAlertRepository.findById(alertId)).willReturn(Optional.of(alert));

            healthAlertService.resolveHealthAlert(alertId, resolvedBy);

            verify(healthAlertRepository).save(captor.capture());
            HealthAlert savedAlert = captor.getValue();
            assertThat(savedAlert.getResolvedAt()).isNotNull();
        } else if ("RESOLVE_NOT_FOUND".equals(action)) {
            UUID alertId = UUID.randomUUID();
            UUID resolvedBy = UUID.randomUUID();
            
            given(healthAlertRepository.findById(alertId)).willReturn(Optional.empty());

            assertThatThrownBy(() -> healthAlertService.resolveHealthAlert(alertId, resolvedBy))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.valueOf(expectedError));
        }
    }
}

