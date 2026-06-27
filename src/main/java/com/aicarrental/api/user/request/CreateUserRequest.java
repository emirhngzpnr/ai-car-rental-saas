package com.aicarrental.api.user.request;

import com.aicarrental.domain.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(@NotBlank(message = "First name is required")
                                String firstName,

                                @NotBlank(message = "Last name is required")
                                String lastName,

                                @Email(message = "Email format is invalid")
                                @NotBlank(message = "Email is required")
                                String email,

                                @NotNull(message = "User role is required")
                                Role role,

                                Long tenantId )


{
}
