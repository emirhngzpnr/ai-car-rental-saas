package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerLoginRequest;
import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.api.customer.response.CustomerAuthResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomerAuthService {
    private final CustomerAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public CustomerAuthResponse register(CustomerRegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new BusinessException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        CustomerAccount account = repository.save(CustomerAccount.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .phone(request.phone().trim())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return response(account);
    }

    public CustomerAuthResponse login(CustomerLoginRequest request) {
        CustomerAccount account = repository.findByEmailIgnoreCaseAndActiveTrue(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        return response(account);
    }

    private CustomerAuthResponse response(CustomerAccount account) {
        return new CustomerAuthResponse(
                jwtService.generateCustomerToken(account),
                "Bearer",
                account.getId(),
                account.getEmail(),
                account.getFirstName(),
                account.getLastName()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
