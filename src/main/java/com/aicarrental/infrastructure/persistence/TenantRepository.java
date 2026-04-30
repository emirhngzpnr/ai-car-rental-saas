package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface    TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsBySubDomain(String subDomain);
    boolean existsBySubDomainAndIdNot(String subDomain, Long id);
}
