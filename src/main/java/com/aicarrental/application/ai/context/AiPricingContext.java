package com.aicarrental.application.ai.context;

import java.math.BigDecimal;

public record AiPricingContext(Long vehicleId,
                               String plateNumber,
                               String brand,
                               String model,
                               BigDecimal currentDailyPrice,
                               Long completedRentalsCount,
                               BigDecimal totalRevenue,
                               BigDecimal extraKmRevenue,
                               BigDecimal refundAmount) {
}
