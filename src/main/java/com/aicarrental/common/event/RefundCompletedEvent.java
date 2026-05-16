package com.aicarrental.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundCompletedEvent(Long paymentId,
                                   Long rentalId,
                                   Long reservationId,
                                   Long tenantId,
                                   BigDecimal refundAmount,
                                   String currency,
                                   LocalDateTime completedAt) {
}
