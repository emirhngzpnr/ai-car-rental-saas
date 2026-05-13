package com.aicarrental.infrastructure.persistence;

import com.aicarrental.domain.insurance.InsurancePackage;
import com.aicarrental.domain.insurance.InsurancePackageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InsurancePackageRepository extends JpaRepository<InsurancePackage, Long> {
    List<InsurancePackage> findByTenant_IdAndActiveTrue(Long tenantId);

    Optional<InsurancePackage> findByIdAndTenant_IdAndActiveTrue(Long id, Long tenantId);

    boolean existsByTenant_IdAndTypeAndActiveTrue(Long tenantId, InsurancePackageType type);}
