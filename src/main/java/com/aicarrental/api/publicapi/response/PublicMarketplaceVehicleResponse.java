package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;

public record PublicMarketplaceVehicleResponse(
        Long vehicleId,
        String tenantSlug,
        String tenantName,
        String brand,
        String model,
        Integer productionYear,
        BigDecimal dailyPrice,
        Integer dailyKmLimit,
        BigDecimal extraKmPricePerKm,
        String category,
        String transmission,
        String fuelType,
        Integer seatCount,
        String location,
        String imageUrl
) {
}
