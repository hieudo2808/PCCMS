package com.astral.express.pccms.notification.dto.response;

import com.astral.express.pccms.notification.entity.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID recipientUserId,
        String sourceType,
        UUID sourceId,
        String notificationType,
        String title,
        String body,
        NotificationStatus statusCode,
        OffsetDateTime readAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
