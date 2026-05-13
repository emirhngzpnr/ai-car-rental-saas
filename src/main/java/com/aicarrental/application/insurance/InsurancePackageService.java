package com.aicarrental.application.insurance;

import com.aicarrental.api.insurance.request.CreateInsurancePackageRequest;
import com.aicarrental.api.insurance.request.UpdateInsurancePackageRequest;
import com.aicarrental.api.insurance.response.InsurancePackageResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.insurance.InsurancePackage;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.InsurancePackageRepository;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsurancePackageService {
    private final InsurancePackageRepository insurancePackageRepository;
    private final TenantRepository tenantRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;

    public InsurancePackageResponse createInsurancePackage(CreateInsurancePackageRequest request) {

        boolean alreadyExists =
                insurancePackageRepository.existsByTenant_IdAndTypeAndActiveTrue(
                        request.tenantId(),
                        request.type()
                );

        if (alreadyExists) {
            throw new BusinessException(
                    "Insurance package already exists for this tenant and type"
            );
        }

        Tenant tenant = tenantRepository.findById(request.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        LocalDateTime now = LocalDateTime.now();

        InsurancePackage insurancePackage = InsurancePackage.builder()
                .tenant(tenant)
                .type(request.type())
                .name(request.name())
                .coverageDescription(request.coverageDescription())
                .dailyPrice(request.dailyPrice())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        InsurancePackage saved =
                insurancePackageRepository.save(insurancePackage);

        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                tenant.getId(),
                AuditAction.INSURANCE_PACKAGE_CREATED,
                "InsurancePackage",
                saved.getId(),
                "Insurance package created: " + saved.getName()
        ));

        return mapToResponse(saved);
    }

    public List<InsurancePackageResponse> getAllInsurancePackages(Long tenantId) {

        return insurancePackageRepository.findByTenant_IdAndActiveTrue(tenantId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public InsurancePackageResponse updateInsurancePackage(
            Long id,
            UpdateInsurancePackageRequest request
    ) {

        InsurancePackage insurancePackage =
                insurancePackageRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Insurance package not found"));

        insurancePackage.setType(request.type());
        insurancePackage.setName(request.name());
        insurancePackage.setCoverageDescription(request.coverageDescription());
        insurancePackage.setDailyPrice(request.dailyPrice());
        insurancePackage.setActive(request.active());
        insurancePackage.setUpdatedAt(LocalDateTime.now());

        InsurancePackage updated =
                insurancePackageRepository.save(insurancePackage);

        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                updated.getTenant() != null ? updated.getTenant().getId() : null,
                AuditAction.INSURANCE_PACKAGE_UPDATED,
                "InsurancePackage",
                updated.getId(),
                "Insurance package updated: " + updated.getName()
        ));

        return mapToResponse(updated);
    }

    public void deleteInsurancePackage(Long id) {

        InsurancePackage insurancePackage =
                insurancePackageRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Insurance package not found"));

        insurancePackage.setActive(false);
        insurancePackage.setUpdatedAt(LocalDateTime.now());

        InsurancePackage deleted =
                insurancePackageRepository.save(insurancePackage);

        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                deleted.getTenant() != null ? deleted.getTenant().getId() : null,
                AuditAction.INSURANCE_PACKAGE_DELETED,
                "InsurancePackage",
                deleted.getId(),
                "Insurance package deleted: " + deleted.getName()
        ));
    }

    private InsurancePackageResponse mapToResponse(
            InsurancePackage insurancePackage
    ) {

        return new InsurancePackageResponse(
                insurancePackage.getId(),

                insurancePackage.getTenant() != null
                        ? insurancePackage.getTenant().getId()
                        : null,

                insurancePackage.getTenant() != null
                        ? insurancePackage.getTenant().getCompanyName()
                        : null,

                insurancePackage.getType(),
                insurancePackage.getName(),
                insurancePackage.getCoverageDescription(),
                insurancePackage.getDailyPrice(),
                insurancePackage.getActive(),
                insurancePackage.getCreatedAt(),
                insurancePackage.getUpdatedAt()
        );

    }
}