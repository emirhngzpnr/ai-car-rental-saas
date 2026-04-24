package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
