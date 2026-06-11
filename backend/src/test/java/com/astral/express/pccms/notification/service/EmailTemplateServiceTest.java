package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EmailTemplateServiceTest {

    @Mock
    private MailProperties mailProperties;

    @InjectMocks
    private EmailTemplateService emailTemplateService;

    @Test
    void should_FormatAccountCreatedContentCorrectly_when_TC_NOTIF_TPL_001() {
        // GIVEN
        given(mailProperties.getAppName()).willReturn("TestApp");
        given(mailProperties.getTeamName()).willReturn("TestTeam");

        String email = "test@example.com";
        String password = "TempPassword123";

        // WHEN
        String subject = emailTemplateService.buildAccountCreatedSubject();
        String content = emailTemplateService.buildAccountCreatedContent(email, password);

        // THEN
        assertThat(subject).contains("TestApp");
        assertThat(content)
                .contains("TestApp")
                .contains(email)
                .contains(password)
                .contains("TestTeam");
    }

    @Test
    void should_FormatTemporaryPasswordContentCorrectly_when_TC_NOTIF_TPL_002() {
        // GIVEN
        given(mailProperties.getAppName()).willReturn("TestApp");
        given(mailProperties.getTeamName()).willReturn("TestTeam");

        String email = "user@example.com";
        String password = "NewTempPassword456";

        // WHEN
        String subject = emailTemplateService.buildTemporaryPasswordSubject();
        String content = emailTemplateService.buildTemporaryPasswordContent(email, password);

        // THEN
        assertThat(subject).contains("TestApp");
        assertThat(content)
                .contains("TestApp")
                .contains(email)
                .contains(password)
                .contains("TestTeam");
    }
}
