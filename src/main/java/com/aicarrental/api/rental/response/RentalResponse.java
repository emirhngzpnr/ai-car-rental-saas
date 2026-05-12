package com.aicarrental.api.rental.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RentalResponse(Long id,

                             Long reservationId,

                             Long tenantId,
                             String tenantName,

                             Long vehicleId,
                             String vehicleBrand,
                             String vehicleModel,
                             String vehiclePlateNumber,

                             LocalDateTime actualPickupDateTime,
                             LocalDateTime actualReturnDateTime,

                             Integer startMileage,
                             Integer endMileage,

                             Integer usedKm,
                             Integer allowedKm,
                             Integer extraKm,

                             BigDecimal extraKmFee,
                             BigDecimal finalRentalPrice,
                             BigDecimal depositDeduction,
                             BigDecimal refundAmount,

                             String status,
                             Boolean active,

                             LocalDateTime createdAt,
                             LocalDateTime updatedAt) {
}
