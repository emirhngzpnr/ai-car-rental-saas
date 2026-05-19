package com.aicarrental.infrastructure.persistence.projection;

import java.math.BigDecimal;

public interface AiPricingProjection {
    Long getVehicleId();

    String getPlateNumber();

    String getBrand();

    String getModel();

    BigDecimal getCurrentDailyPrice();

    Long getCompletedRentalsCount();

    BigDecimal getTotalRevenue();

    BigDecimal getExtraKmRevenue();

    BigDecimal getRefundAmount();
}
