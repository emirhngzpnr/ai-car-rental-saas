package com.aicarrental.domain.reservation;

public enum ReservationStatus {
    PENDING_PAYMENT,

    DEPOSIT_PAID,

    CONFIRMED,

    CONVERTED_TO_RENTAL,

    COMPLETED,

    CANCELLED,

    EXPIRED
}
