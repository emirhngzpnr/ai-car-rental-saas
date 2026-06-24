package com.aicarrental.infrastructure.persistence;

public interface VehiclePriceDistributionProjection {
    long getSampleCount();

    Double getP30();

    Double getP45();

    Double getP60();

    Double getP70();

    Double getP75();
}
