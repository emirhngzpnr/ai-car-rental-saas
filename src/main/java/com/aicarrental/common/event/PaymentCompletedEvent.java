package com.aicarrental.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCompletedEvent(
        Long paymentId,
        Long reservationId,
        Long tenantId,
        BigDecimal amount,
        String currency,
        String paymentType,
        LocalDateTime completedAt
) {
}
