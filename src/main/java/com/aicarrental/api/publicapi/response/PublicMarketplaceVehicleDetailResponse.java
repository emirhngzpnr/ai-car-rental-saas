package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;
import java.util.List;

public record PublicMarketplaceVehicleDetailResponse(
        Long vehicleId,
        String tenantSlug,
        String tenantName,
        String tenantEmail,
        String tenantPhone,
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
        String imageUrl,
        BigDecimal averageRating,
        Long reviewCount,
        List<PublicInsurancePackageResponse> insurancePackages
) {
}
