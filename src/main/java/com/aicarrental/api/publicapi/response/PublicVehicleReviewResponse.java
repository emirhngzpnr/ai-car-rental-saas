package com.aicarrental.api.publicapi.response;

import java.time.LocalDateTime;

public record PublicVehicleReviewResponse(
        Long id,
        Integer rating,
        String title,
        String comment,
        String customerDisplayName,
        LocalDateTime createdAt
) {
}
