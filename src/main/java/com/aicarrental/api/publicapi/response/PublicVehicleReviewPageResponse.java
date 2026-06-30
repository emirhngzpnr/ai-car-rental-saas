package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicVehicleReviewPageResponse(
        List<PublicVehicleReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
