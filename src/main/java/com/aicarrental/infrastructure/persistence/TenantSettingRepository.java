package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.tenant.TenantSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TenantSettingRepository extends JpaRepository<TenantSetting, Long> {
    List<TenantSetting> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<TenantSetting> findByTenant_IdAndSettingKeyAndActiveTrue(
            Long tenantId,
            String settingKey
    );

    boolean existsByTenant_IdAndSettingKey(
            Long tenantId,
            String settingKey
    );
}
