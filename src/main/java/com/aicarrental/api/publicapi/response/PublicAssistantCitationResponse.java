package com.aicarrental.api.publicapi.response;

public record PublicAssistantCitationResponse(
        String title,
        String category,
        String tenantName
) {
}
