package com.aicarrental.application.auth;

import com.aicarrental.api.auth.request.LoginRequest;
import com.aicarrental.api.auth.response.AuthResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.auth.User;
import com.aicarrental.infrastructure.persistence.UserRepository;
import com.aicarrental.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditEventPublisher auditEventPublisher;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    auditEventPublisher.publish(new AuditEvent(
                            null,
                            request.email(),
                            null,
                            null,
                            AuditAction.LOGIN_FAILED,
                            "Auth",
                            null,
                            "Login failed: user not found"
                    ));

                    return new BusinessException("Invalid email or password");
                });

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            auditEventPublisher.publish(new AuditEvent(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name(),
                    user.getTenant() != null ? user.getTenant().getId() : null,
                    AuditAction.LOGIN_FAILED,
                    "Auth",
                    user.getId(),
                    "Login failed: invalid password"
            ));

            throw new BusinessException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        auditEventPublisher.publish(new AuditEvent(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenantId,
                AuditAction.LOGIN_SUCCESS,
                "Auth",
                user.getId(),
                "Login successful: " + user.getEmail()
        ));

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenantId
        );
    }
}
