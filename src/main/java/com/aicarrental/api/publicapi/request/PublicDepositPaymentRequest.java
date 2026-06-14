package com.aicarrental.api.publicapi.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record PublicDepositPaymentRequest(
        @Email(message = "Customer email format is invalid")
        @NotBlank(message = "Customer email is required")
        String email,

        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey
) {
}
