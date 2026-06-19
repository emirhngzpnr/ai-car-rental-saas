package com.aicarrental.api.customer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CustomerLoginRequest(@Email @NotBlank String email, @NotBlank String password) {
}
