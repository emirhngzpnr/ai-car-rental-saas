package com.aicarrental.api.customer.response;

import java.time.LocalDateTime;

public record CustomerVehicleReviewResponse(
        Long id,
        String reservationCode,
        Long vehicleId,
        String vehicleBrand,
        String vehicleModel,
        Integer rating,
        String title,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
