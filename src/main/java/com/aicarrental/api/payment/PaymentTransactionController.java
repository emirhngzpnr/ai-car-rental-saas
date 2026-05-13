package com.aicarrental.api.payment;

import com.aicarrental.api.payment.request.CreatePaymentRequest;
import com.aicarrental.api.payment.response.PaymentTransactionResponse;
import com.aicarrental.application.payment.PaymentTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentTransactionController {
    private final PaymentTransactionService paymentTransactionService;

    @PostMapping
    public ResponseEntity<PaymentTransactionResponse> createPayment(
            @Valid @RequestBody CreatePaymentRequest request
    ) {
        PaymentTransactionResponse response =
                paymentTransactionService.createPayment(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
