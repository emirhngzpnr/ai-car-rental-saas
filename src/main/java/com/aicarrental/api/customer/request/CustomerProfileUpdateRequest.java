package com.aicarrental.api.customer.request;

import jakarta.validation.constraints.NotBlank;

public record CustomerProfileUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String phone
) {
}
