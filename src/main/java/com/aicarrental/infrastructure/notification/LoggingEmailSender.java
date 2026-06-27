package com.aicarrental.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "false", matchIfMissing = true)
public class LoggingEmailSender implements EmailSender {
    @Override
    public void sendEmail(String recipient, String subject, String message) {
        log.info("""
                        
                EMAIL DELIVERY DISABLED
                TO: {}
                SUBJECT: {}
                MESSAGE: {}
                        
                """,
                recipient,
                subject,
                message
        );
    }

    @Override
    public void sendEmailWithAttachment(
            String recipient,
            String subject,
            String message,
            String attachmentFilename,
            byte[] attachmentContent,
            String attachmentContentType
    ) {
        log.info("""
                        
                EMAIL DELIVERY DISABLED
                TO: {}
                SUBJECT: {}
                MESSAGE: {}
                ATTACHMENT: {} ({} bytes, {})
                        
                """,
                recipient,
                subject,
                message,
                attachmentFilename,
                attachmentContent != null ? attachmentContent.length : 0,
                attachmentContentType
        );
    }
}
