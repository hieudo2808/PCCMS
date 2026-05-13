package com.astral.express.pccms.notification.service;

public interface EmailService {
    void sendAccountCreatedEmail(String toEmail, String temporaryPassword);
}