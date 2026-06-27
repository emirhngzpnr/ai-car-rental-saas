package com.aicarrental.api.customer.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerTokenRequest(
        @NotBlank(message = "Token is required")
        String token
) {
}
