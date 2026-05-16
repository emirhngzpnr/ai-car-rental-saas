package com.aicarrental.api.tenant.response;

import com.aicarrental.domain.tenant.TenantSettingDataType;

public record TenantSettingResponse(Long id,
                                    Long tenantId,
                                    String settingKey,
                                    String settingValue,
                                    TenantSettingDataType dataType,
                                    String defaultValue,
                                    String description,
                                    Boolean active) {
}
