package com.aicarrental.api.customer.response;

public record CustomerRegistrationResponse(
        String email,
        String message,
        boolean verificationRequired
) {
}
