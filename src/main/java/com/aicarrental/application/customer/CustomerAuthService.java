package com.aicarrental.application.customer;

import com.aicarrental.api.customer.request.CustomerLoginRequest;
import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.api.customer.response.CustomerMessageResponse;
import com.aicarrental.api.customer.response.CustomerAuthResponse;
import com.aicarrental.api.customer.response.CustomerRegistrationResponse;
import com.aicarrental.common.exception.BusinessException;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.domain.customer.CustomerAccountToken;
import com.aicarrental.domain.customer.CustomerAccountTokenType;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.RefreshTokenService;
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
    private final CustomerAccountTokenService tokenService;
    private final CustomerEmailService emailService;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public CustomerRegistrationResponse register(CustomerRegisterRequest request, String requestIp) {
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
                .emailVerified(false)
                .createdAt(now)
                .updatedAt(now)
                .build());

        String rawToken = tokenService.createEmailVerificationToken(account, requestIp);

        try {
            emailService.sendVerificationEmail(account.getEmail(), rawToken);
            account.setLastVerificationEmailSentAt(now);
            repository.save(account);
            return new CustomerRegistrationResponse(
                    account.getEmail(),
                    "Registration successful. Please check your email to verify your account.",
                    true
            );
        } catch (Exception exception) {
            account.setLastVerificationEmailSentAt(null);
            repository.save(account);
            return new CustomerRegistrationResponse(
                    account.getEmail(),
                    "Registration successful, but verification email could not be sent. Please request a new verification email.",
                    true
            );
        }
    }

    public CustomerAuthResponse login(CustomerLoginRequest request) {
        CustomerAccount account = repository.findByEmailIgnoreCaseAndActiveTrue(normalizeEmail(request.email()))
                .orElseThrow(() -> new BusinessException("Invalid email or password"));
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException("Invalid email or password");
        }
        if (!Boolean.TRUE.equals(account.getEmailVerified())) {
            throw new BusinessException("Please verify your email before signing in.");
        }
        return createAuthResponse(account);
    }

    @Transactional
    public CustomerMessageResponse verifyEmail(String rawToken) {
        CustomerAccountToken token = tokenService.consume(rawToken, CustomerAccountTokenType.EMAIL_VERIFICATION);
        CustomerAccount customer = token.getCustomerAccount();
        customer.setEmailVerified(true);
        customer.setEmailVerifiedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        repository.save(customer);
        return new CustomerMessageResponse("Email verified successfully.");
    }

    @Transactional
    public CustomerMessageResponse resendVerification(String email, String requestIp) {
        repository.findByEmailIgnoreCaseAndActiveTrue(normalizeEmail(email))
                .filter(account -> !Boolean.TRUE.equals(account.getEmailVerified()))
                .ifPresent(account -> {
                    LocalDateTime now = LocalDateTime.now();
                    if (account.getLastVerificationEmailSentAt() != null
                            && account.getLastVerificationEmailSentAt().isAfter(now.minusSeconds(60))) {
                        throw new BusinessException("Please wait before requesting another verification email.");
                    }

                    String rawToken = tokenService.createEmailVerificationToken(account, requestIp);
                    emailService.sendVerificationEmail(account.getEmail(), rawToken);
                    account.setLastVerificationEmailSentAt(now);
                    account.setUpdatedAt(now);
                    repository.save(account);
                });

        return new CustomerMessageResponse("If verification is required, a new email has been sent.");
    }

    @Transactional
    public CustomerMessageResponse forgotPassword(String email, String requestIp) {
        repository.findByEmailIgnoreCaseAndActiveTrue(normalizeEmail(email))
                .filter(account -> Boolean.TRUE.equals(account.getEmailVerified()))
                .ifPresent(account -> {
                    String rawToken = tokenService.createPasswordResetToken(account, requestIp);
                    emailService.sendPasswordResetEmail(account.getEmail(), rawToken);
                });

        return new CustomerMessageResponse("If an account exists, a reset link has been sent.");
    }

    @Transactional
    public CustomerMessageResponse resetPassword(String rawToken, String newPassword) {
        CustomerAccountToken token = tokenService.consume(rawToken, CustomerAccountTokenType.PASSWORD_RESET);
        CustomerAccount customer = token.getCustomerAccount();
        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customer.setUpdatedAt(LocalDateTime.now());
        repository.save(customer);
        refreshTokenService.revokeAll(RefreshTokenPrincipalType.CUSTOMER, customer.getId());
        return new CustomerMessageResponse("Password updated successfully.");
    }

    public CustomerAuthResponse createAuthResponse(CustomerAccount account) {
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
