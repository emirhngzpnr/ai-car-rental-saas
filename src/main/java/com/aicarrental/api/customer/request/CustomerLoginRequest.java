package com.aicarrental.api.customer.request;

import com.aicarrental.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerLoginRequest(
        @Email(regexp = ValidationPatterns.EMAIL, message = "Email format is invalid")
        @NotBlank
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,
        @NotBlank String password
) {
}
