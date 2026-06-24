package com.aicarrental.infrastructure.security;

import com.aicarrental.common.exception.UnauthorizedException;
import com.aicarrental.domain.auth.RefreshToken;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.infrastructure.persistence.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    public static final String STAFF_COOKIE_NAME = "acr_staff_refresh";
    public static final String CUSTOMER_COOKIE_NAME = "acr_customer_refresh";

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public String issueToken(RefreshTokenPrincipalType principalType, Long principalId) {
        return createToken(principalType, principalId);
    }

    @Transactional
    public RotationResult rotate(String rawToken, RefreshTokenPrincipalType principalType) {
        RefreshToken currentToken = findUsableToken(rawToken, principalType);
        currentToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(currentToken);

        String nextRawToken = createToken(principalType, currentToken.getPrincipalId());
        return new RotationResult(currentToken.getPrincipalId(), nextRawToken);
    }

    private String createToken(RefreshTokenPrincipalType principalType, Long principalId) {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        refreshTokenRepository.save(RefreshToken.builder()
                .principalType(principalType)
                .principalId(principalId)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .build());

        return rawToken;
    }

    @Transactional
    public void revoke(String rawToken, RefreshTokenPrincipalType principalType) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }

        refreshTokenRepository
                .findByTokenHashAndPrincipalTypeAndRevokedAtIsNull(hash(rawToken), principalType)
                .ifPresent(token -> {
                    token.setRevokedAt(LocalDateTime.now());
                    refreshTokenRepository.save(token);
                });
    }

    public long getRefreshExpirationSeconds() {
        return Duration.ofMillis(refreshExpirationMs).toSeconds();
    }

    private RefreshToken findUsableToken(String rawToken, RefreshTokenPrincipalType principalType) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new UnauthorizedException("Refresh session is missing");
        }

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashAndPrincipalTypeAndRevokedAtIsNull(hash(rawToken), principalType)
                .orElseThrow(() -> new UnauthorizedException("Refresh session is invalid"));

        if (!refreshToken.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh session has expired");
        }

        return refreshToken;
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Refresh token hashing failed", exception);
        }
    }

    public record RotationResult(Long principalId, String rawToken) {
    }
}
