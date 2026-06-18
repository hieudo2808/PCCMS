package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.dto.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class WebSocketNotificationPublisher implements NotificationPublisher {
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(NotificationResponse response) {
        Runnable publish = () -> messagingTemplate.convertAndSendToUser(
                response.recipientUserId().toString(),
                "/queue/notifications",
                response
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish.run();
                }
            });
            return;
        }
        publish.run();
    }
}
