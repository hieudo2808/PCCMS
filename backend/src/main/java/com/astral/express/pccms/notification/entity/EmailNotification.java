package com.astral.express.pccms.notification.entity;

import com.astral.express.pccms.notification.enums.EmailNotificationStatus;
import com.astral.express.pccms.notification.enums.EmailNotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_notifications")
public class EmailNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 255)
    private String recipientEmail;

    @Column(nullable = false, length = 255)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmailNotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EmailNotificationStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = EmailNotificationStatus.PENDING;
        }
    }

    public void markSent() {
        this.status = EmailNotificationStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.errorMessage = null;
    }

    public void markFailed(String errorMessage) {
        this.status = EmailNotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }
}