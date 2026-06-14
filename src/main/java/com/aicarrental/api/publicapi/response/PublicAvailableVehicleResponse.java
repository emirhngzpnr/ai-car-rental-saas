package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;

public record PublicAvailableVehicleResponse(
        Long vehicleId,
        String brand,
        String model,
        Integer productionYear,
        String maskedPlateNumber,
        BigDecimal dailyPrice,
        Integer dailyKmLimit,
        BigDecimal extraKmPricePerKm
) {
}
