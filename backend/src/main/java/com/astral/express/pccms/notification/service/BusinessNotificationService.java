package com.astral.express.pccms.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BusinessNotificationService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final NotificationService notificationService;

    public void appointmentConfirmed(UUID recipientId, UUID appointmentId, String petName, OffsetDateTime scheduledAt) {
        create(recipientId, "APPOINTMENT", appointmentId, "APPOINTMENT_CONFIRMED",
                "Lịch hẹn đã được xác nhận",
                "Lịch hẹn của " + petName + formatTime(scheduledAt) + " đã được xác nhận.");
    }

    public void appointmentCancelled(UUID recipientId, UUID appointmentId, String petName) {
        create(recipientId, "APPOINTMENT", appointmentId, "APPOINTMENT_CANCELLED",
                "Lịch hẹn đã bị hủy", "Lịch hẹn của " + petName + " đã bị hủy.");
    }

    public void appointmentCompleted(UUID recipientId, UUID appointmentId, String petName) {
        create(recipientId, "APPOINTMENT", appointmentId, "APPOINTMENT_COMPLETED",
                "Lịch khám đã hoàn tất", "Lịch khám của " + petName + " đã hoàn tất.");
    }

    public void groomingCompleted(UUID recipientId, UUID ticketId, String petName) {
        create(recipientId, "GROOMING", ticketId, "GROOMING_COMPLETED",
                "Dịch vụ làm đẹp đã hoàn tất", petName + " đã hoàn tất dịch vụ làm đẹp và sẵn sàng được đón.");
    }

    public void groomingCancelled(UUID recipientId, UUID ticketId, String petName) {
        create(recipientId, "GROOMING", ticketId, "GROOMING_CANCELLED",
                "Dịch vụ làm đẹp đã bị hủy", "Lịch làm đẹp của " + petName + " đã bị hủy.");
    }

    public void boardingCheckedIn(UUID recipientId, UUID bookingId, String petName) {
        create(recipientId, "BOARDING", bookingId, "BOARDING_CHECKED_IN",
                "Đã nhận thú cưng lưu trú", petName + " đã được nhận vào khu lưu trú.");
    }

    public void boardingCheckedOut(UUID recipientId, UUID bookingId, String petName) {
        create(recipientId, "BOARDING", bookingId, "BOARDING_CHECKED_OUT",
                "Lưu trú đã hoàn tất", petName + " đã hoàn tất kỳ lưu trú.");
    }

    public void boardingCancelled(UUID recipientId, UUID bookingId, String petName) {
        create(recipientId, "BOARDING", bookingId, "BOARDING_CANCELLED",
                "Đặt phòng lưu trú đã bị hủy", "Đặt phòng lưu trú của " + petName + " đã bị hủy.");
    }

    public void invoiceIssued(UUID recipientId, UUID invoiceId, String petName, long amountVnd) {
        create(recipientId, "INVOICE", invoiceId, "INVOICE_ISSUED",
                "Hóa đơn mới đã được phát hành",
                "Hóa đơn cho " + safePetName(petName) + " có tổng tiền " + money(amountVnd) + " đ.");
    }

    public void invoicePaid(UUID recipientId, UUID invoiceId, String petName, long amountVnd) {
        create(recipientId, "INVOICE", invoiceId, "INVOICE_PAID",
                "Hóa đơn đã được thanh toán",
                "Đã thanh toán đủ " + money(amountVnd) + " đ cho hóa đơn của " + safePetName(petName) + ".");
    }

    private void create(UUID recipientId, String sourceType, UUID sourceId, String type, String title, String body) {
        notificationService.createNotification(recipientId, sourceType, sourceId, type, title, body);
    }

    private String formatTime(OffsetDateTime scheduledAt) {
        return scheduledAt == null ? "" : " lúc " + scheduledAt.format(DATE_TIME_FORMAT);
    }

    private String money(long amountVnd) {
        return NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amountVnd);
    }

    private String safePetName(String petName) {
        return petName == null || petName.isBlank() ? "dịch vụ thú cưng" : petName;
    }
}
