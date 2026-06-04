package com.aicarrental.domain.outbox;

public enum OutboxEventType {

    PAYMENT_COMPLETED,

    RESERVATION_CREATED,

    RESERVATION_CONFIRMED,

    RESERVATION_EXPIRED,

    RENTAL_COMPLETED,

    REFUND_COMPLETED,

    AI_PRICING_APPROVED

}
