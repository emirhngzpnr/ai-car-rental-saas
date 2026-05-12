package com.aicarrental.api.reservation.response;


import java.math.BigDecimal;
import java.time.LocalDateTime;
public record ReservationResponse(
        Long id,

        Long tenantId,
        String tenantName,

        Long vehicleId,
        String vehicleBrand,
        String vehicleModel,
        String vehiclePlateNumber,

        String customerFullName,
        String customerPhone,
        String customerEmail,
        String customerIdentityNumber,

        LocalDateTime pickupDateTime,
        LocalDateTime returnDateTime,

        BigDecimal dailyPriceSnapshot,
        Integer dailyKmLimitSnapshot,
        BigDecimal extraKmPricePerKmSnapshot,

        BigDecimal depositAmount,
        BigDecimal estimatedRentalPrice,
        BigDecimal totalEstimatedPrice,

        String status,
        Boolean active,

        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
