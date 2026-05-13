package com.astral.express.pccms.notification.service.impl;

import com.astral.express.pccms.notification.config.MailProperties;
import com.astral.express.pccms.notification.entity.EmailNotification;
import com.astral.express.pccms.notification.enums.EmailNotificationStatus;
import com.astral.express.pccms.notification.enums.EmailNotificationType;
import com.astral.express.pccms.notification.repository.EmailNotificationRepository;
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
    private final EmailNotificationRepository emailNotificationRepository;

    @Async("mailTaskExecutor")
    @Override
    public void sendAccountCreatedEmail(String toEmail, String temporaryPassword) {
        String subject = emailTemplateService.buildAccountCreatedSubject();

        EmailNotification notification = EmailNotification.builder()
                .recipientEmail(toEmail)
                .subject(subject)
                .type(EmailNotificationType.ACCOUNT_CREATED)
                .status(EmailNotificationStatus.PENDING)
                .build();

        notification = emailNotificationRepository.save(notification);

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

            notification.markSent();
            emailNotificationRepository.save(notification);

            log.info("Account creation email sent to: {}", toEmail);
        } catch (MailException exception) {
            notification.markFailed(exception.getMessage());
            emailNotificationRepository.save(notification);

            log.error("Failed to send account creation email to: {}", toEmail, exception);
        }
    }
}