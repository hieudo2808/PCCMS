package com.astral.express.pccms.notification.repository;

import com.astral.express.pccms.notification.entity.Notification;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdAndStatusCodeInOrderByCreatedAtDesc(
            UUID recipientUserId, Set<NotificationStatus> statuses, Pageable pageable);

    Page<Notification> findByRecipientIdAndStatusCodeOrderByCreatedAtDesc(
            UUID recipientUserId, NotificationStatus status, Pageable pageable);

    long countByRecipientIdAndStatusCode(UUID recipientUserId, NotificationStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification notification
            SET notification.statusCode = com.astral.express.pccms.notification.entity.NotificationStatus.READ,
                notification.readAt = :readAt
            WHERE notification.recipient.id = :recipientUserId
              AND notification.statusCode = com.astral.express.pccms.notification.entity.NotificationStatus.UNREAD
            """)
    int markAllRead(@Param("recipientUserId") UUID recipientUserId, @Param("readAt") OffsetDateTime readAt);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientUserId);
}
