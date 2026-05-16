package com.aicarrental.infrastructure.persistence.projection;

import java.math.BigDecimal;

public interface TopVehicleProjection {
    Long getVehicleId();

    String getPlateNumber();

    String getBrand();

    String getModel();

    Long getRentalCount();

    BigDecimal getTotalRevenue();
}
