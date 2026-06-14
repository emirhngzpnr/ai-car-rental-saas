package com.aicarrental.application.publicapi;

import com.aicarrental.api.publicapi.response.PublicTenantResponse;
import com.aicarrental.common.exception.ResourceNotFoundException;
import com.aicarrental.domain.tenant.Tenant;
import com.aicarrental.infrastructure.persistence.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicTenantService {
    private final TenantRepository tenantRepository;

    public PublicTenantResponse getTenant(String tenantSlug) {
        Tenant tenant = findActiveTenant(tenantSlug);
        return new PublicTenantResponse(
                tenant.getSlug(),
                tenant.getCompanyName(),
                tenant.getEmail(),
                tenant.getPhoneNumber()
        );
    }

    Tenant findActiveTenant(String tenantSlug) {
        return tenantRepository.findBySlugAndActiveTrue(normalizeSlug(tenantSlug))
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));
    }

    private String normalizeSlug(String tenantSlug) {
        return tenantSlug == null ? "" : tenantSlug.trim().toLowerCase();
    }
}
