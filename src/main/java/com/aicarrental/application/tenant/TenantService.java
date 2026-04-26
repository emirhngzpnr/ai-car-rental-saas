package com.aicarrental.application.tenant;

import com.aicarrental.api.tenant.request.CreateTenantRequest;
import com.aicarrental.api.tenant.response.TenantResponse;
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
    }
