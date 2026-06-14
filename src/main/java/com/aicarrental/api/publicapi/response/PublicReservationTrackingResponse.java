package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicReservationTrackingResponse(
        String reservationCode,
        String status,
        String vehicleBrand,
        String vehicleModel,
        String maskedPlateNumber,
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,
        BigDecimal depositAmount,
        BigDecimal totalEstimatedPrice,
        String paymentStatusSummary
) {
}
