package com.aicarrental.common.event;

import java.time.LocalDateTime;

public record RentalCompletedEvent(Long rentalId,
                                   Long reservationId,
                                   Long tenantId,
                                   LocalDateTime completedAt) {
}
