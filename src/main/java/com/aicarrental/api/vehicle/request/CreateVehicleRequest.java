package com.aicarrental.api.vehicle.request;

import com.aicarrental.domain.vehicle.VehicleStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateVehicleRequest(@NotBlank(message = "Brand is required")
                                   String brand,

                                   @NotBlank(message = "Model is required")
                                   String model,

                                   @NotBlank(message = "Plate number is required")
                                   String plateNumber,

                                   @NotNull(message = "Production year is required")
                                   @Min(value = 2000, message = "Production year is invalid")
                                   Integer productionYear,

                                   @NotNull(message = "Current mileage is required")
                                   @Min(value = 0, message = "Current mileage cannot be negative")
                                   Integer currentMileage,

                                   @NotNull(message = "Daily price is required")
                                   @DecimalMin(value = "0.0", inclusive = false, message = "Daily price must be greater than 0")
                                   BigDecimal dailyPrice,

                                   @NotNull(message = "Daily kilometer limit is required")
                                   @Min(value = 1, message = "Daily kilometer limit must be at least 1")
                                   Integer dailyKmLimit,

                                   @NotNull(message = "Extra kilometer price is required")
                                   @DecimalMin(value = "0.0", inclusive = false, message = "Extra kilometer price must be greater than 0")
                                   BigDecimal extraKmPricePerKm,

                                   @NotNull(message = "Vehicle status is required")
                                   VehicleStatus status) {
}
