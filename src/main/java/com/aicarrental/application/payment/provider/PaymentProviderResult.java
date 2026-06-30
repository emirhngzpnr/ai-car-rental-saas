package com.aicarrental.application.payment.provider;

public record PaymentProviderResult(
        String providerName,
        String providerTransactionId
) {
}
