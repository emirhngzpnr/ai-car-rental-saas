package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicReservationResponse(
        String reservationCode,
        String status,
        String customerEmail,
        String vehicleBrand,
        String vehicleModel,
        String maskedPlateNumber,
        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,
        BigDecimal depositAmount,
        BigDecimal estimatedRentalPrice,
        BigDecimal insuranceTotalPrice,
        BigDecimal totalEstimatedPrice
) {
}
