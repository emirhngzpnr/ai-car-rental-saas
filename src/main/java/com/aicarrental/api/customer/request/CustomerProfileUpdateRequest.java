package com.aicarrental.api.customer.request;

import com.aicarrental.common.validation.ValidPhoneNumber;
import jakarta.validation.constraints.NotBlank;

public record CustomerProfileUpdateRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @ValidPhoneNumber String phone
) {
}
