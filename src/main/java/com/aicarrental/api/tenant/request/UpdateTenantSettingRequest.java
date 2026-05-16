package com.aicarrental.api.tenant.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateTenantSettingRequest(
                                        @NotBlank
                                         String settingValue) {
}
