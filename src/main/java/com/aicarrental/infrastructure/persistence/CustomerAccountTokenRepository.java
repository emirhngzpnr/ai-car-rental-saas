package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.customer.CustomerAccountToken;
import com.aicarrental.domain.customer.CustomerAccountTokenType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface CustomerAccountTokenRepository extends JpaRepository<CustomerAccountToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<CustomerAccountToken> findByTokenHashAndTypeAndUsedAtIsNull(
            String tokenHash,
            CustomerAccountTokenType type
    );
}
