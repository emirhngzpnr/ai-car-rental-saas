package com.aicarrental.api.publicapi.response;

import com.aicarrental.domain.vehicle.FuelType;
import com.aicarrental.domain.vehicle.TransmissionType;
import com.aicarrental.domain.vehicle.VehicleCategory;

import java.math.BigDecimal;
import java.util.List;

public record PublicVehicleSearchCriteriaResponse(
        BigDecimal minDailyPrice,
        BigDecimal maxDailyPrice,
        Integer minDailyKmLimit,
        String brand,
        String model,
        List<VehicleCategory> categories,
        TransmissionType transmission,
        FuelType fuelType,
        Integer minSeats,
        String location,
        String sort
) {
}
