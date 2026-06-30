package com.aicarrental.application.payment.provider;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class DemoPaymentProvider implements PaymentProvider {
    public static final String PROVIDER_NAME = "DEMO";

    @Override
    public PaymentProviderResult chargeDeposit(BigDecimal amount, String currency, String idempotencyKey) {
        return success("DEMO-DEPOSIT");
    }

    @Override
    public PaymentProviderResult chargeRentalPayment(BigDecimal amount, String currency, String idempotencyKey) {
        return success("DEMO-RENTAL");
    }

    @Override
    public PaymentProviderResult refund(BigDecimal amount, String currency, String idempotencyKey) {
        return success("DEMO-REFUND");
    }

    private PaymentProviderResult success(String prefix) {
        return new PaymentProviderResult(
                PROVIDER_NAME,
                prefix + "-" + System.currentTimeMillis()
        );
    }
}
