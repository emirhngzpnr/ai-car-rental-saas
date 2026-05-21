package com.aicarrental.api.vehicle.response;

import java.math.BigDecimal;

public record AvailableVehicleResponse(Long id,
                                       String plateNumber,
                                       String brand,
                                       String model,
                                       BigDecimal dailyPrice,
                                       Integer currentMileage,
                                       String status) {
}
