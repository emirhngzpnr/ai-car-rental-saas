package com.aicarrental.api.vehicle.request;

import com.aicarrental.domain.vehicle.VehicleStatus;

import java.math.BigDecimal;

public record UpdateVehicleRequest(String brand,
                                   String model,
                                   String plateNumber,
                                   Integer productionYear,
                                   Integer currentMileage,
                                   BigDecimal dailyPrice,
                                   Integer dailyKmLimit,
                                   BigDecimal extraKmPricePerKm,
                                   VehicleStatus status,
                                   Boolean active) {
}
