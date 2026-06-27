package com.aicarrental.infrastructure.notification;

public interface EmailSender {
    void sendEmail(String recipient, String subject, String message);
}
