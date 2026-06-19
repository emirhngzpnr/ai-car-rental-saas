package com.aicarrental.api.customer.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerReservationResponse(
        String reservationCode,
        String status,
        String tenantSlug,
        String tenantName,
        Long vehicleId,
        String vehicleBrand,
        String vehicleModel,
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,
        BigDecimal depositAmount,
        BigDecimal estimatedRentalPrice,
        BigDecimal insuranceTotalPrice,
        BigDecimal totalEstimatedPrice,
        String paymentStatusSummary
) {
}
