package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;
import java.util.List;

public record PublicVehicleDetailResponse(
        Long vehicleId,
        String brand,
        String model,
        Integer productionYear,
        String maskedPlateNumber,
        BigDecimal dailyPrice,
        Integer dailyKmLimit,
        BigDecimal extraKmPricePerKm,
        List<PublicInsurancePackageResponse> insurancePackages
) {
}
