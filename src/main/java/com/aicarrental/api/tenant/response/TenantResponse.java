package com.aicarrental.api.tenant.response;

import java.time.LocalDateTime;

public record TenantResponse(Long id,
                             String companyName,
                             String subDomain,
                             String slug,
                             Boolean active,
                             String email,
                             String phoneNumber,
                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
}
