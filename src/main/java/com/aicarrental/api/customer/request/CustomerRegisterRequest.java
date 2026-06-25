package com.aicarrental.api.customer.request;

import com.aicarrental.common.validation.ValidPhoneNumber;
import com.aicarrental.common.validation.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerRegisterRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @Email(regexp = ValidationPatterns.EMAIL, message = "Email format is invalid")
        @NotBlank
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,
        @NotBlank @Size(min = 8, message = "Password must contain at least 8 characters") String password,
        @NotBlank @ValidPhoneNumber String phone
) {
}
