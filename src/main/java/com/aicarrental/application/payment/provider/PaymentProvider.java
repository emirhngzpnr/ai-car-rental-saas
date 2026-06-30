package com.aicarrental.application.payment.provider;

import java.math.BigDecimal;

public interface PaymentProvider {
    PaymentProviderResult chargeDeposit(BigDecimal amount, String currency, String idempotencyKey);

    PaymentProviderResult chargeRentalPayment(BigDecimal amount, String currency, String idempotencyKey);

    PaymentProviderResult refund(BigDecimal amount, String currency, String idempotencyKey);
}
