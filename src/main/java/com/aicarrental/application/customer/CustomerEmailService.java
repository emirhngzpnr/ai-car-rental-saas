package com.aicarrental.application.customer;

import com.aicarrental.infrastructure.notification.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class CustomerEmailService {
    private final EmailSender emailSender;

    @Value("${app.frontend.customer-base-url}")
    private String customerBaseUrl;

    public void sendVerificationEmail(String recipient, String rawToken) {
        String link = customerBaseUrl + "/customer/verify-email?token=" + encode(rawToken);
        emailSender.sendEmail(
                recipient,
                "Verify your AI Car Rental account",
                """
                        Welcome to AI Car Rental.
                        
                        Please verify your email address using this link:
                        %s
                        
                        This link expires in 24 hours.
                        """.formatted(link)
        );
    }

    public void sendPasswordResetEmail(String recipient, String rawToken) {
        String link = customerBaseUrl + "/customer/reset-password?token=" + encode(rawToken);
        emailSender.sendEmail(
                recipient,
                "Reset your AI Car Rental password",
                """
                        You requested a password reset for your AI Car Rental account.
                        
                        Set a new password using this link:
                        %s
                        
                        This link expires in 30 minutes. If you did not request this, ignore this email.
                        """.formatted(link)
        );
    }

    private String encode(String rawToken) {
        return URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
    }
}
