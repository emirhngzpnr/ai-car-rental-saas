package com.aicarrental.infrastructure.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MockEmailSenderService {
    public void sendEmail(
            String recipient,
            String subject,
            String message
    ) {
        log.info("""
                        
                MOCK EMAIL SENT
                TO: {}
                SUBJECT: {}
                MESSAGE: {}
                        
                """,
                recipient,
                subject,
                message
        );
    }
}
