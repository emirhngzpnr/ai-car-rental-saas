package com.aicarrental.api.tenant.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(@NotBlank(message = "Company name is required")
                                  String companyName,

                                  @NotBlank(message = "Subdomain is required")
                                  String subDomain,

                                  @Email(message = "Email format is invalid")
                                  @NotBlank(message = "Email is required")
                                  String email,

                                  @NotBlank(message = "Phone number is required")
                                  String phoneNumber,

                                  Boolean active) {
}
