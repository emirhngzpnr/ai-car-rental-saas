package com.aicarrental.api.vehicle.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleResponse(Long id,
                              String brand,
                              String model,
                              String plateNumber,
                              Integer productionYear,
                              Integer currentMileage,
                              BigDecimal dailyPrice,
                              Integer dailyKmLimit,
                              BigDecimal extraKmPricePerKm,
                              String category,
                              String transmission,
                              String fuelType,
                              Integer seatCount,
                              String location,
                              String imageUrl,
                              String status,
                              Boolean active,
                              Long tenantId,
                              String tenantName,
                              LocalDateTime createdAt,
                              LocalDateTime updatedAt) {
}
