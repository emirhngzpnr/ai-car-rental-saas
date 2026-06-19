package com.aicarrental.api.customer.response;

public record CustomerAuthResponse(
        String token,
        String tokenType,
        Long customerId,
        String email,
        String firstName,
        String lastName
) {
}
