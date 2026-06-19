package com.aicarrental.api.publicapi.response;

import java.util.List;

public record PublicMarketplaceSearchResponse(
        List<PublicMarketplaceVehicleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
