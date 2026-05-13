package com.aicarrental.api.payment.response;

import com.aicarrental.domain.payment.PaymentStatus;
import com.aicarrental.domain.payment.PaymentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentTransactionResponse(Long id,

                                         Long tenantId,

                                         Long reservationId,

                                         Long rentalId,

                                         PaymentType paymentType,

                                         PaymentStatus paymentStatus,

                                         BigDecimal amount,

                                         String currency,

                                         String providerTransactionId,

                                         String idempotencyKey,

                                         LocalDateTime createdAt,

                                         LocalDateTime updatedAt) {
}
