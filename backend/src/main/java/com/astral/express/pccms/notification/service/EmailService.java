package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.config.MailProperties;
import com.astral.express.pccms.notification.service.EmailService;
import com.astral.express.pccms.notification.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailTemplateService emailTemplateService;

    @Async("mailTaskExecutor")
public void sendAccountCreatedEmail(String toEmail, String temporaryPassword) {
        String subject = emailTemplateService.buildAccountCreatedSubject();

        try {
            String content = emailTemplateService.buildAccountCreatedContent(toEmail, temporaryPassword);

            SimpleMailMessage message = new SimpleMailMessage();

            if (StringUtils.hasText(mailProperties.getFrom())) {
                message.setFrom(mailProperties.getFrom());
            }

            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);

            log.info("Account creation email sent to: {}", toEmail);
        } catch (MailException exception) {
            log.error("Failed to send account creation email to: {}. Cause: {}", toEmail, exception.getMessage());
        }
    }

    @Async("mailTaskExecutor")
public void sendTemporaryPasswordEmail(String toEmail, String temporaryPassword) {
        try {
            String content = emailTemplateService.buildTemporaryPasswordContent(toEmail, temporaryPassword);

            SimpleMailMessage message = new SimpleMailMessage();

            if (StringUtils.hasText(mailProperties.getFrom())) {
                message.setFrom(mailProperties.getFrom());
            }

            message.setTo(toEmail);
            message.setSubject(emailTemplateService.buildTemporaryPasswordSubject());
            message.setText(content);

            mailSender.send(message);

            log.info("Temporary password email sent to: {}", toEmail);
        } catch (MailException exception) {
            log.error("Failed to send temporary password email to: {}. Cause: {}", toEmail, exception.getMessage());
        }
    }

    @Async("mailTaskExecutor")
public void sendOtpEmail(String toEmail, String purpose, String otp) {
        if (!StringUtils.hasText(toEmail) || !toEmail.contains("@")) {
            log.info("OTP generated for non-email contact; external SMS sender is not configured");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();

            if (StringUtils.hasText(mailProperties.getFrom())) {
                message.setFrom(mailProperties.getFrom());
            }

            message.setTo(toEmail);
            message.setSubject("PCCMS OTP - " + purpose);
            message.setText("Your PCCMS OTP is: " + otp + "\nThis code expires in 10 minutes.");

            mailSender.send(message);
            log.info("OTP email sent to: {}", toEmail);
        } catch (MailException exception) {
            log.error("Failed to send OTP email to: {}. Cause: {}", toEmail, exception.getMessage());
        }
    }
}


