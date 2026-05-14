package com.aicarrental.domain.outbox;

public enum OutboxMessageStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
