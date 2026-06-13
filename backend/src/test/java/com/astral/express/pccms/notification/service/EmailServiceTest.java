package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.config.MailProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MailProperties mailProperties;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> messageCaptor;

    @Test
    void should_SendAccountCreatedEmail_when_Called_TC_NOTIF_EML_001() {
        // GIVEN
        String toEmail = "test@example.com";
        String password = "temp";
        given(emailTemplateService.buildAccountCreatedSubject()).willReturn("Subject");
        given(emailTemplateService.buildAccountCreatedContent(toEmail, password)).willReturn("Content");
        given(mailProperties.getFrom()).willReturn("admin@example.com");

        // WHEN
        emailService.sendAccountCreatedEmail(toEmail, password);

        // THEN
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly(toEmail);
        assertThat(message.getSubject()).isEqualTo("Subject");
        assertThat(message.getText()).isEqualTo("Content");
        assertThat(message.getFrom()).isEqualTo("admin@example.com");
    }

    @Test
    void should_SendTemporaryPasswordEmail_when_Called_TC_NOTIF_EML_002() {
        // GIVEN
        String toEmail = "user@example.com";
        String password = "new_temp";
        given(emailTemplateService.buildTemporaryPasswordSubject()).willReturn("Subject2");
        given(emailTemplateService.buildTemporaryPasswordContent(toEmail, password)).willReturn("Content2");
        given(mailProperties.getFrom()).willReturn("admin@example.com");

        // WHEN
        emailService.sendTemporaryPasswordEmail(toEmail, password);

        // THEN
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly(toEmail);
        assertThat(message.getSubject()).isEqualTo("Subject2");
        assertThat(message.getText()).isEqualTo("Content2");
        assertThat(message.getFrom()).isEqualTo("admin@example.com");
    }

    @Test
    void should_SendOtpEmail_when_ValidEmailProvided_TC_NOTIF_EML_003() {
        // GIVEN
        String toEmail = "otp@example.com";
        String purpose = "Login";
        String otp = "123456";
        given(mailProperties.getFrom()).willReturn("noreply@example.com");

        // WHEN
        emailService.sendOtpEmail(toEmail, purpose, otp);

        // THEN
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertThat(message.getTo()).containsExactly(toEmail);
        assertThat(message.getSubject()).contains(purpose);
        assertThat(message.getText()).contains(otp);
        assertThat(message.getFrom()).isEqualTo("noreply@example.com");
    }

    @Test
    void should_SkipSendingOtp_when_EmailIsInvalid_TC_NOTIF_EML_004() {
        // GIVEN
        String phone = "0901234567"; // Invalid email (no @)

        // WHEN
        emailService.sendOtpEmail(phone, "Login", "123456");

        // THEN
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void should_HandleMailExceptionGracefully_when_MailSenderFails_TC_NOTIF_EML_005() {
        // GIVEN
        String toEmail = "fail@example.com";
        String password = "temp";
        given(emailTemplateService.buildAccountCreatedSubject()).willReturn("Subject");
        given(emailTemplateService.buildAccountCreatedContent(toEmail, password)).willReturn("Content");
        
        willThrow(new MailSendException("SMTP error")).given(mailSender).send(any(SimpleMailMessage.class));

        // WHEN & THEN (Assert no exception is thrown)
        assertThatCode(() -> emailService.sendAccountCreatedEmail(toEmail, password))
                .doesNotThrowAnyException();
    }

    @Test
    void should_HandleMailException_when_SendTemporaryPasswordEmailFails() {
        String toEmail = "fail@example.com";
        String password = "temp";
        given(emailTemplateService.buildTemporaryPasswordSubject()).willReturn("Subject");
        given(emailTemplateService.buildTemporaryPasswordContent(toEmail, password)).willReturn("Content");
        
        willThrow(new MailSendException("SMTP error")).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendTemporaryPasswordEmail(toEmail, password))
                .doesNotThrowAnyException();
    }

    @Test
    void should_SendTemporaryPasswordEmail_when_FromIsBlank() {
        String toEmail = "user@example.com";
        String password = "new_temp";
        given(emailTemplateService.buildTemporaryPasswordSubject()).willReturn("Subject2");
        given(emailTemplateService.buildTemporaryPasswordContent(toEmail, password)).willReturn("Content2");
        given(mailProperties.getFrom()).willReturn("");

        emailService.sendTemporaryPasswordEmail(toEmail, password);

        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isNull();
    }

    @Test
    void should_HandleMailException_when_SendOtpEmailFails() {
        String toEmail = "otp@example.com";
        willThrow(new MailSendException("SMTP error")).given(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(() -> emailService.sendOtpEmail(toEmail, "Login", "123"))
                .doesNotThrowAnyException();
    }

    @Test
    void should_SendOtpEmail_when_FromIsBlank() {
        String toEmail = "otp@example.com";
        given(mailProperties.getFrom()).willReturn(null);

        emailService.sendOtpEmail(toEmail, "Login", "123");

        verify(mailSender).send(messageCaptor.capture());
        assertThat(messageCaptor.getValue().getFrom()).isNull();
    }

}
