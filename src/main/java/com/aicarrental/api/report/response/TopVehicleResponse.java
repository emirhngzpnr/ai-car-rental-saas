package com.aicarrental.api.report.response;

import java.math.BigDecimal;

public record TopVehicleResponse(Long vehicleId,
                                 String plateNumber,
                                 String brand,
                                 String model,
                                 Long rentalCount,
                                 BigDecimal totalRevenue) {
}
