package com.aicarrental.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationCreatedEvent(Long reservationId,
                                      Long tenantId,
                                      Long vehicleId,
                                      String customerFullName,
                                      String customerEmail,
                                      String plateNumber,
                                      String vehicleBrand,
                                      String vehicleModel,
                                      LocalDateTime pickupDateTime,
                                      LocalDateTime returnDateTime,
                                      BigDecimal totalEstimatedPrice,
                                      LocalDateTime createdAt) {
}
