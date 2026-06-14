package com.aicarrental.api.publicapi.response;

public record PublicTenantResponse(
        String slug,
        String companyName,
        String email,
        String phoneNumber
) {
}
