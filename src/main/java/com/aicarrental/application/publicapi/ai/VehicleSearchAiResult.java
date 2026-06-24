package com.aicarrental.application.publicapi.ai;

import java.math.BigDecimal;
import java.util.List;

public record VehicleSearchAiResult(
        BigDecimal minDailyPrice,
        BigDecimal maxDailyPrice,
        Integer minDailyKmLimit,
        String brand,
        String model,
        String category,
        String transmission,
        String fuelType,
        Integer minSeats,
        String location,
        String sort,
        String priceIntent,
        String segmentIntent,
        String summary,
        List<String> warnings
) {
}
