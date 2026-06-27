package com.aicarrental.application.customer;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.customer.CustomerAccountToken;
import com.aicarrental.domain.customer.CustomerAccountTokenType;
import com.aicarrental.infrastructure.persistence.CustomerAccountTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class CustomerAccountTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration EMAIL_VERIFICATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);

    private final CustomerAccountTokenRepository repository;

    public String createEmailVerificationToken(CustomerAccount customer, String requestIp) {
        return createToken(customer, CustomerAccountTokenType.EMAIL_VERIFICATION, EMAIL_VERIFICATION_TTL, requestIp);
    }

    public String createPasswordResetToken(CustomerAccount customer, String requestIp) {
        return createToken(customer, CustomerAccountTokenType.PASSWORD_RESET, PASSWORD_RESET_TTL, requestIp);
    }

    public CustomerAccountToken consume(String rawToken, CustomerAccountTokenType type) {
        CustomerAccountToken token = repository
                .findByTokenHashAndTypeAndUsedAtIsNull(hash(rawToken), type)
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Invalid or expired token");
        }

        token.setUsedAt(LocalDateTime.now());
        return repository.save(token);
    }

    private String createToken(CustomerAccount customer, CustomerAccountTokenType type, Duration ttl, String requestIp) {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        repository.save(CustomerAccountToken.builder()
                .customerAccount(customer)
                .tokenHash(hash(rawToken))
                .type(type)
                .expiresAt(LocalDateTime.now().plus(ttl))
                .createdAt(LocalDateTime.now())
                .requestIp(requestIp)
                .build());

        return rawToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Customer token hashing failed", exception);
        }
    }
}
