package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import com.astral.express.pccms.notification.entity.NotificationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketNotificationPublisherTest {
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void should_publishNotificationToUserQueue() {
        WebSocketNotificationPublisher publisher = new WebSocketNotificationPublisher(messagingTemplate);
        UUID recipientId = UUID.randomUUID();
        NotificationResponse response = new NotificationResponse(
                UUID.randomUUID(),
                recipientId,
                "SYSTEM",
                null,
                "ALERT",
                "Title",
                "Body",
                NotificationStatus.UNREAD,
                null,
                null,
                null);

        publisher.publish(response);

        verify(messagingTemplate).convertAndSendToUser(
                recipientId.toString(),
                "/queue/notifications",
                response);
    }
}
