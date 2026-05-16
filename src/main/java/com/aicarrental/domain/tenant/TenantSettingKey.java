package com.aicarrental.domain.tenant;

public enum TenantSettingKey {
    INVOICE_PREFIX(
            TenantSettingDataType.STRING,
            "INV"
    ),

    DEFAULT_CURRENCY(
            TenantSettingDataType.STRING,
            "TRY"
    ),

    RESERVATION_EXPIRATION_MINUTES(
            TenantSettingDataType.INTEGER,
            "30"
    ),

    EMAIL_NOTIFICATIONS_ENABLED(
            TenantSettingDataType.BOOLEAN,
            "true"
    ),

    EXTRA_KM_DEFAULT_PRICE(
            TenantSettingDataType.DECIMAL,
            "10.00"
    ),

    AI_PRICING_ENABLED(
            TenantSettingDataType.BOOLEAN,
            "false"
    );

    private final TenantSettingDataType dataType;
    private final String defaultValue;

    TenantSettingKey(
            TenantSettingDataType dataType,
            String defaultValue
    ) {
        this.dataType = dataType;
        this.defaultValue = defaultValue;
    }

    public TenantSettingDataType getDataType() {
        return dataType;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
