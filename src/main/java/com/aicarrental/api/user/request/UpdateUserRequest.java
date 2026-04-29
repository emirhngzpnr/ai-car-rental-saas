package com.aicarrental.api.user.request;

import com.aicarrental.domain.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        String firstName,

        String lastName,

        @Email(message = "Email format is invalid")
        String email,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        Role role,

        Boolean active) {
}
