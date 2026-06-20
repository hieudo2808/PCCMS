package com.astral.express.pccms.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BusinessNotificationServiceTest {
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BusinessNotificationService businessNotificationService;

    @Test
    void groomingCompleted_shouldCreateTypedOwnerNotification() {
        UUID ownerId = UUID.randomUUID();
        UUID ticketId = UUID.randomUUID();

        businessNotificationService.groomingCompleted(ownerId, ticketId, "Milu");

        verify(notificationService).createNotification(
                eq(ownerId), eq("GROOMING"), eq(ticketId), eq("GROOMING_COMPLETED"),
                contains("hoàn tất"), contains("Milu"));
    }

    @Test
    void invoicePaid_shouldIncludeFormattedAmount() {
        UUID ownerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        businessNotificationService.invoicePaid(ownerId, invoiceId, "Milu", 250_000L);

        verify(notificationService).createNotification(
                eq(ownerId), eq("INVOICE"), eq(invoiceId), eq("INVOICE_PAID"),
                contains("thanh toán"), contains("250.000"));
    }
}
