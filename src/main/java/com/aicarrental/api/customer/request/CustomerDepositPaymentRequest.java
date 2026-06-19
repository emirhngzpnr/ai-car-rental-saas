package com.aicarrental.api.customer.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerDepositPaymentRequest(@NotBlank String idempotencyKey) {
}
