package com.aicarrental.infrastructure.persistence.projection;

public interface VehicleReviewSummaryProjection {
    Long getVehicleId();

    Double getAverageRating();

    Long getReviewCount();
}
