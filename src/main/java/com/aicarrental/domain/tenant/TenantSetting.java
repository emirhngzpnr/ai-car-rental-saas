package com.aicarrental.domain.tenant;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "tenant_settings",
        schema = "rental",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_tenant_settings_tenant_key",
                        columnNames = {"tenant_,id","setting_key"}
                )
        },
        indexes = {
                @Index(name = "idx_tenant_setting_tenant_id",columnList = "tenant_id"),
                @Index(name = "idx_tenant_settings_setting_key",columnList = "setting_key")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", nullable = false, length = 500)
    private String settingValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.active == null) {
            this.active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
