package com.astral.express.pccms.notification.repository;

import com.astral.express.pccms.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientUserId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientUserId);
}
