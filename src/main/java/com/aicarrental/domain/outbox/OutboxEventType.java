package com.aicarrental.domain.outbox;

public enum OutboxEventType {

    PAYMENT_COMPLETED,

    RESERVATION_CONFIRMED,

    RESERVATION_EXPIRED,

    RENTAL_COMPLETED,

    REFUND_COMPLETED

}
