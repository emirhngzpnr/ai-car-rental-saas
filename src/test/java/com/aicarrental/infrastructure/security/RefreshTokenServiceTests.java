package com.aicarrental.infrastructure.security;

import com.aicarrental.common.exception.UnauthorizedException;
import com.aicarrental.domain.auth.RefreshToken;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.infrastructure.persistence.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTests {
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService(refreshTokenRepository);
        ReflectionTestUtils.setField(service, "refreshExpirationMs", 604_800_000L);
    }

    @Test
    void rotateRevokesCurrentTokenAndIssuesReplacementInSameOperation() {
        RefreshToken currentToken = RefreshToken.builder()
                .id(10L)
                .principalType(RefreshTokenPrincipalType.STAFF)
                .principalId(42L)
                .tokenHash("current-token-hash")
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        when(refreshTokenRepository.findByTokenHashAndPrincipalTypeAndRevokedAtIsNull(
                any(String.class),
                any(RefreshTokenPrincipalType.class)
        )).thenReturn(Optional.of(currentToken));
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotationResult result = service.rotate(
                "current-raw-token",
                RefreshTokenPrincipalType.STAFF
        );

        assertEquals(42L, result.principalId());
        assertNotNull(result.rawToken());
        assertNotEquals("current-raw-token", result.rawToken());
        assertNotNull(currentToken.getRevokedAt());

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(captor.capture());
        List<RefreshToken> savedTokens = captor.getAllValues();

        RefreshToken replacement = savedTokens.get(1);
        assertEquals(RefreshTokenPrincipalType.STAFF, replacement.getPrincipalType());
        assertEquals(42L, replacement.getPrincipalId());
        assertEquals(64, replacement.getTokenHash().length());
        assertNotNull(replacement.getExpiresAt());
    }

    @Test
    void rotateRejectsExpiredRefreshToken() {
        RefreshToken expiredToken = RefreshToken.builder()
                .principalType(RefreshTokenPrincipalType.CUSTOMER)
                .principalId(7L)
                .tokenHash("expired-token-hash")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenRepository.findByTokenHashAndPrincipalTypeAndRevokedAtIsNull(
                any(String.class),
                any(RefreshTokenPrincipalType.class)
        )).thenReturn(Optional.of(expiredToken));

        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate("expired-raw-token", RefreshTokenPrincipalType.CUSTOMER)
        );
    }

    @Test
    void rotateRejectsMissingRefreshCookie() {
        assertThrows(
                UnauthorizedException.class,
                () -> service.rotate(null, RefreshTokenPrincipalType.STAFF)
        );
    }
}
