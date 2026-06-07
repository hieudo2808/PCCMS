package com.astral.express.pccms.notification.service.impl;

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
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final EmailTemplateService emailTemplateService;

    @Async("mailTaskExecutor")
    @Override
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
}