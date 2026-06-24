package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.auth.RefreshToken;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHashAndPrincipalTypeAndRevokedAtIsNull(
            String tokenHash,
            RefreshTokenPrincipalType principalType
    );
}
