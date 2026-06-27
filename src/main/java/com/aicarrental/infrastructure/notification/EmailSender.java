package com.aicarrental.infrastructure.notification;

public interface EmailSender {
    void sendEmail(String recipient, String subject, String message);

    void sendEmailWithAttachment(
            String recipient,
            String subject,
            String message,
            String attachmentFilename,
            byte[] attachmentContent,
            String attachmentContentType
    );
}
