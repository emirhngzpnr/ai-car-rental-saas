package com.aicarrental.application.tenant;

import com.aicarrental.api.tenant.request.CreateTenantRequest;
import com.aicarrental.api.tenant.request.UpdateTenantRequest;
import com.aicarrental.api.tenant.response.TenantResponse;
import com.aicarrental.common.audit.AuditAction;
import com.aicarrental.common.audit.AuditEvent;
import com.aicarrental.common.audit.AuditEventPublisher;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.common.security.CurrentUserService;
import com.aicarrental.domain.auth.User;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {
    private final TenantRepository tenantRepository;
    private final AuditEventPublisher auditEventPublisher;
    private final CurrentUserService currentUserService;

    public TenantResponse createTenant(CreateTenantRequest request) {
        if (tenantRepository.existsBySubDomain(request.subDomain())) {
            throw new BusinessException("Subdomain already exists");
        }
        Tenant tenant = Tenant.builder()
                .companyName(request.companyName())
                .subDomain(request.subDomain())
                .email(request.email())
                .phoneNumber(request.phoneNumber())
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);
        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                savedTenant.getId(),
                AuditAction.TENANT_CREATED,
                "Tenant",
                savedTenant.getId(),
                "Tenant created: " + savedTenant.getCompanyName()
        ));

        return mapToResponse(savedTenant);
        }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        return mapToResponse(tenant);
    }
    public TenantResponse updateTenant(Long id, UpdateTenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (tenantRepository.existsBySubDomainAndIdNot(request.subDomain(), id)) {
            throw new BusinessException("Subdomain already exists");
        }

        tenant.setCompanyName(request.companyName());
        tenant.setSubDomain(request.subDomain());
        tenant.setEmail(request.email());
        tenant.setPhoneNumber(request.phoneNumber());

        if (request.active() != null) {
            tenant.setActive(request.active());
        }
        tenant.setUpdatedAt(LocalDateTime.now());
        Tenant updatedTenant = tenantRepository.save(tenant);
        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                updatedTenant.getId(),
                AuditAction.TENANT_UPDATED,
                "Tenant",
                updatedTenant.getId(),
                "Tenant updated: " + updatedTenant.getCompanyName()
        ));

        return mapToResponse(updatedTenant);
    }

    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        tenant.setActive(false);
        tenant.setUpdatedAt(LocalDateTime.now());

        tenantRepository.save(tenant);
        User currentUser = currentUserService.getCurrentUser();

        auditEventPublisher.publish(new AuditEvent(
                currentUser.getId(),
                currentUser.getEmail(),
                currentUser.getRole().name(),
                tenant.getId(),
                AuditAction.TENANT_DELETED,
                "Tenant",
                tenant.getId(),
                "Tenant soft deleted: " + tenant.getCompanyName()
        ));
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCompanyName(),
                tenant.getSubDomain(),
                tenant.getActive(),
                tenant.getEmail(),
                tenant.getPhoneNumber(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }

    }
