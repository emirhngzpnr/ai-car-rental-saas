package com.aicarrental.api.publicapi.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicDepositPaymentResponse(
        String reservationCode,
        String reservationStatus,
        String paymentStatus,
        BigDecimal amount,
        String currency,
        LocalDateTime paidAt
) {
}
