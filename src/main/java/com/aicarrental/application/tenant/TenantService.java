package com.aicarrental.application.tenant;

import com.aicarrental.api.tenant.request.CreateTenantRequest;
import com.aicarrental.api.tenant.request.UpdateTenantRequest;
import com.aicarrental.api.tenant.response.TenantResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantService {
    private final TenantRepository tenantRepository;

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
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        return new TenantResponse(
                savedTenant.getId(),
                savedTenant.getCompanyName(),
                savedTenant.getSubDomain(),
                savedTenant.getActive(),
                savedTenant.getEmail(),
                savedTenant.getPhoneNumber(),
                savedTenant.getCreatedAt()
            );
        }

    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll()
                .stream()
                .map(tenant -> new TenantResponse(
                        tenant.getId(),
                        tenant.getCompanyName(),
                        tenant.getSubDomain(),
                        tenant.getActive(),
                        tenant.getEmail(),
                        tenant.getPhoneNumber(),
                        tenant.getCreatedAt()
                ))
                .toList();
    }
    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
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

        Tenant updatedTenant = tenantRepository.save(tenant);

        return mapToResponse(updatedTenant);
    }

    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        tenantRepository.delete(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCompanyName(),
                tenant.getSubDomain(),
                tenant.getActive(),
                tenant.getEmail(),
                tenant.getPhoneNumber(),
                tenant.getCreatedAt()
        );
    }
    }
