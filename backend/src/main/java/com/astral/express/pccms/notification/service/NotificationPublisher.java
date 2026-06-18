package com.astral.express.pccms.notification.service;

import com.astral.express.pccms.notification.dto.response.NotificationResponse;

public interface NotificationPublisher {
    void publish(NotificationResponse response);
}
