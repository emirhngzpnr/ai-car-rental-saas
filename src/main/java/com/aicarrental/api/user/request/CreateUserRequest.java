package com.aicarrental.api.user.request;

import com.aicarrental.domain.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank(message = "First name is required")
                                String firstName,

                                @NotBlank(message = "Last name is required")
                                String lastName,

                                @Email(message = "Email format is invalid")
                                @NotBlank(message = "Email is required")
                                String email,

                                @NotBlank(message = "Password is required")
                                @Size(min = 6, message = "Password must be at least 6 characters")
                                String password,

                                @NotNull(message = "User role is required")
                                Role role,

                                Long tenantId )


{
}
