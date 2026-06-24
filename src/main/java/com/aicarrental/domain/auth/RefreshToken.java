package com.aicarrental.domain.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "refresh_tokens",
        schema = "rental",
        indexes = {
                @Index(name = "idx_refresh_tokens_principal", columnList = "principal_type, principal_id"),
                @Index(name = "idx_refresh_tokens_expires_at", columnList = "expires_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "principal_type", nullable = false, length = 20)
    private RefreshTokenPrincipalType principalType;

    @Column(name = "principal_id", nullable = false)
    private Long principalId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
