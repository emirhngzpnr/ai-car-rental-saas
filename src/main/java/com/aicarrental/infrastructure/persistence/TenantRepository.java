package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.tenant.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface    TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsBySubDomain(String subDomain);
    boolean existsBySubDomainAndIdNot(String subDomain, Long id);
    List<Tenant> findByActiveTrue();

    Optional<Tenant> findByIdAndActiveTrue(Long id);
}
