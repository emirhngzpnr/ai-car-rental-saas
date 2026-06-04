package com.aicarrental.common.event;

import java.time.LocalDateTime;

public record ReservationExpiredEvent(Long reservationId,
                                      Long tenantId,
                                      Long vehicleId,
                                      String customerFullName,
                                      String customerEmail,
                                      String plateNumber,
                                      String vehicleBrand,
                                      String vehicleModel,
                                      LocalDateTime expiredAt) {
}
