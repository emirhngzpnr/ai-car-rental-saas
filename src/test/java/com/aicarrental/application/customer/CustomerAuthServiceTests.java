package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.customer.CustomerAccountToken;
import com.aicarrental.domain.customer.CustomerAccountTokenType;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.JwtService;
import com.aicarrental.infrastructure.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerAuthServiceTests {
    @Mock CustomerAccountRepository repository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock CustomerAccountTokenService tokenService;
    @Mock CustomerEmailService emailService;
    @Mock RefreshTokenService refreshTokenService;
    private CustomerAuthService service;

    @BeforeEach
    void setUp() {
        service = new CustomerAuthService(repository, passwordEncoder, jwtService, tokenService, emailService, refreshTokenService);
    }

    @Test
    void registerRejectsDuplicateEmailCaseInsensitively() {
        when(repository.existsByEmailIgnoreCase("customer@example.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.register(new CustomerRegisterRequest(
                "Ada", "Lovelace", "Customer@Example.com", "password123", "+905551112233"
        ), "127.0.0.1"));
    }

    @Test
    void registerCreatesUnverifiedCustomerAndSendsVerificationEmail() {
        CustomerAccount saved = CustomerAccount.builder()
                .id(1L)
                .firstName("Ada")
                .lastName("Lovelace")
                .email("customer@example.com")
                .active(true)
                .emailVerified(false)
                .build();
        when(repository.save(any(CustomerAccount.class))).thenReturn(saved);
        when(tokenService.createEmailVerificationToken(saved, "127.0.0.1")).thenReturn("raw-token");

        var response = service.register(new CustomerRegisterRequest(
                "Ada", "Lovelace", "Customer@Example.com", "password123", "+905551112233"
        ), "127.0.0.1");

        assertThat(response.verificationRequired()).isTrue();
        assertThat(response.email()).isEqualTo("customer@example.com");
        verify(emailService).sendVerificationEmail("customer@example.com", "raw-token");
    }

    @Test
    void loginRejectsUnverifiedCustomer() {
        CustomerAccount customer = CustomerAccount.builder()
                .email("customer@example.com")
                .passwordHash("hash")
                .active(true)
                .emailVerified(false)
                .build();
        when(repository.findByEmailIgnoreCaseAndActiveTrue("customer@example.com")).thenReturn(Optional.of(customer));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.login(new com.aicarrental.api.customer.request.CustomerLoginRequest(
                "customer@example.com",
                "password123"
        )));
    }

    @Test
    void verifyEmailMarksCustomerVerified() {
        CustomerAccount customer = CustomerAccount.builder()
                .id(7L)
                .email("customer@example.com")
                .emailVerified(false)
                .build();
        CustomerAccountToken token = CustomerAccountToken.builder()
                .customerAccount(customer)
                .type(CustomerAccountTokenType.EMAIL_VERIFICATION)
                .build();
        when(tokenService.consume("raw-token", CustomerAccountTokenType.EMAIL_VERIFICATION)).thenReturn(token);

        var response = service.verifyEmail("raw-token");

        assertThat(response.message()).isEqualTo("Email verified successfully.");
        assertThat(customer.getEmailVerified()).isTrue();
        verify(repository).save(customer);
    }

    @Test
    void resetPasswordUpdatesHashAndRevokesCustomerRefreshTokens() {
        CustomerAccount customer = CustomerAccount.builder()
                .id(9L)
                .email("customer@example.com")
                .passwordHash("old-hash")
                .build();
        CustomerAccountToken token = CustomerAccountToken.builder()
                .customerAccount(customer)
                .type(CustomerAccountTokenType.PASSWORD_RESET)
                .build();
        when(tokenService.consume("reset-token", CustomerAccountTokenType.PASSWORD_RESET)).thenReturn(token);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        var response = service.resetPassword("reset-token", "new-password");

        assertThat(response.message()).isEqualTo("Password updated successfully.");
        assertThat(customer.getPasswordHash()).isEqualTo("new-hash");
        verify(repository).save(customer);
        verify(refreshTokenService).revokeAll(RefreshTokenPrincipalType.CUSTOMER, 9L);
    }
}
