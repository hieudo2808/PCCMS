package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.common.dto.PageResponse;
import com.astral.express.pccms.common.exception.BusinessException;
import com.astral.express.pccms.common.exception.ErrorCode;
import com.astral.express.pccms.identity.security.SecurityContextService;
import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import com.astral.express.pccms.notification.dto.response.ReadAllNotificationsResponse;
import com.astral.express.pccms.notification.dto.response.UnreadCountResponse;
import com.astral.express.pccms.notification.entity.Notification;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import com.astral.express.pccms.notification.repository.NotificationRepository;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SecurityContextService securityContextService;
    private final NotificationPublisher notificationPublisher;

    @Transactional
    public NotificationResponse createNotification(
            UUID recipientUserId,
            String sourceType,
            UUID sourceId,
            String notificationType,
            String title,
            String body) {
        Users recipient = userRepository.findById(recipientUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_ACC_002_USER_NOT_FOUND));
        Notification notification = Notification.builder()
                .recipient(recipient)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .notificationType(notificationType)
                .title(title)
                .body(body)
                .statusCode(NotificationStatus.UNREAD)
                .build();
        NotificationResponse response = toResponse(notificationRepository.save(notification));
        notificationPublisher.publish(response);
        return response;
    }
    public PageResponse<NotificationResponse> listMyNotifications(NotificationStatus status, Pageable pageable) {
        UUID currentUserId = requireCurrentUserId();
        if (status != null) {
            return PageResponse.of(notificationRepository
                    .findByRecipientIdAndStatusCodeOrderByCreatedAtDesc(currentUserId, status, pageable)
                    .map(this::toResponse));
        }
        return PageResponse.of(notificationRepository
                .findByRecipientIdAndStatusCodeInOrderByCreatedAtDesc(
                        currentUserId, Set.of(NotificationStatus.UNREAD, NotificationStatus.READ), pageable)
                .map(this::toResponse));
    }

    public UnreadCountResponse getUnreadCount() {
        return new UnreadCountResponse(notificationRepository.countByRecipientIdAndStatusCode(
                requireCurrentUserId(), NotificationStatus.UNREAD));
    }

    @Transactional
    public ReadAllNotificationsResponse markAllRead() {
        int updatedCount = notificationRepository.markAllRead(requireCurrentUserId(), OffsetDateTime.now());
        return new ReadAllNotificationsResponse(updatedCount);
    }

    @Transactional
    public NotificationResponse markRead(UUID notificationId) {
        Notification notification = findMine(notificationId);
        if (notification.getStatusCode() != NotificationStatus.UNREAD) {
            return toResponse(notification);
        }
        notification.setStatusCode(NotificationStatus.READ);
        notification.setReadAt(OffsetDateTime.now());
        return toResponse(notificationRepository.save(notification));
    }
    @Transactional
    public NotificationResponse archive(UUID notificationId) {
        Notification notification = findMine(notificationId);
        notification.setStatusCode(NotificationStatus.ARCHIVED);
        if (notification.getReadAt() == null) {
            notification.setReadAt(OffsetDateTime.now());
        }
        return toResponse(notificationRepository.save(notification));
    }

    private Notification findMine(UUID notificationId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, requireCurrentUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ERR_NOTIFICATION_001_NOT_FOUND));
    }

    private UUID requireCurrentUserId() {
        UUID currentUserId = securityContextService.getCurrentUserId();
        if (currentUserId == null) {
            throw new BusinessException(ErrorCode.ERR_401_UNAUTHORIZED);
        }
        return currentUserId;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient().getId(),
                notification.getSourceType(),
                notification.getSourceId(),
                notification.getNotificationType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getStatusCode(),
                notification.getReadAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}


