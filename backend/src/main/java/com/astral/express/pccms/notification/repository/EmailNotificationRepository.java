package com.astral.express.pccms.notification.repository;

import com.astral.express.pccms.notification.entity.EmailNotification;
import com.astral.express.pccms.notification.enums.EmailNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {
    List<EmailNotification> findByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    List<EmailNotification> findByStatusOrderByCreatedAtDesc(EmailNotificationStatus status);
}