package com.aicarrental.api.customer;

import com.aicarrental.api.customer.request.CustomerLoginRequest;
import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.api.customer.response.CustomerAuthResponse;
import com.aicarrental.application.customer.CustomerAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final CustomerAuthService authService;

    @PostMapping("/register")
    public ResponseEntity<CustomerAuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
