package com.aicarrental.api.customer.response;

public record CustomerProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone
) {
}
