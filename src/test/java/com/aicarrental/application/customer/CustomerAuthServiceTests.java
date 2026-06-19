package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTests {
    @Mock CustomerAccountRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthService(repository, passwordEncoder, jwtService);
    }

    @Test
    void registerRejectsDuplicateEmailCaseInsensitively() {
        when(repository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.register(new CustomerRegisterRequest(
                "Ada", "Lovelace", "Customer@Example.com", "password123", "+905551112233"
        )));
    }
}
