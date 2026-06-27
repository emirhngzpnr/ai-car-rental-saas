package com.aicarrental.application.user;

import com.aicarrental.domain.auth.User;
import com.aicarrental.infrastructure.notification.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class UserInvitationEmailService {
    private final EmailSender emailSender;

    @Value("${app.frontend.staff-base-url}")
    private String staffBaseUrl;

    public void sendInvitationEmail(User user, String rawToken) {
        String link = staffBaseUrl + "/set-password?token=" + encode(rawToken);
        String tenantName = user.getTenant() == null ? "AI Car Rental" : user.getTenant().getCompanyName();

        emailSender.sendEmail(
                user.getEmail(),
                "Set your AI Car Rental account password",
                """
                        An account has been created for you on AI Car Rental.

                        Company: %s
                        Role: %s

                        Set your password using this one-time link:
                        %s

                        This link expires in 24 hours. If you did not expect this invitation, you can ignore this email.
                        """.formatted(tenantName, user.getRole().name(), link)
        );
    }

    private String encode(String value) {
        return UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8);
    }
}
