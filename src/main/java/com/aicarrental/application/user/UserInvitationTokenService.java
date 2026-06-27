package com.aicarrental.application.user;

import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.auth.UserInvitationToken;
import com.aicarrental.infrastructure.persistence.UserInvitationTokenRepository;
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
public class UserInvitationTokenService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration INVITATION_TTL = Duration.ofHours(24);

    private final UserInvitationTokenRepository repository;

    public String createInvitationToken(User user, User invitedByUser) {
        byte[] randomBytes = new byte[64];
        SECURE_RANDOM.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        repository.save(UserInvitationToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(LocalDateTime.now().plus(INVITATION_TTL))
                .createdAt(LocalDateTime.now())
                .invitedByUser(invitedByUser)
                .build());

        return rawToken;
    }

    public UserInvitationToken consume(String rawToken) {
        UserInvitationToken token = repository
                .findByTokenHashAndUsedAtIsNull(hash(rawToken))
                .orElseThrow(() -> new BusinessException("Invalid or expired invitation link"));

        if (!token.getExpiresAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("Invalid or expired invitation link");
        }

        token.setUsedAt(LocalDateTime.now());
        return repository.save(token);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("User invitation token hashing failed", exception);
        }
    }
}
