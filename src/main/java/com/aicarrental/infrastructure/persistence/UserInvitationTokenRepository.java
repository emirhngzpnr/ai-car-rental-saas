package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.auth.UserInvitationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserInvitationTokenRepository extends JpaRepository<UserInvitationToken, Long> {
    Optional<UserInvitationToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
}
