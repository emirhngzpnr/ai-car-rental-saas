package com.aicarrental.api.customer;

import com.aicarrental.api.customer.request.CustomerLoginRequest;
import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.api.customer.request.CustomerEmailRequest;
import com.aicarrental.api.customer.request.CustomerResetPasswordRequest;
import com.aicarrental.api.customer.request.CustomerTokenRequest;
import com.aicarrental.api.customer.response.CustomerAuthResponse;
import com.aicarrental.api.customer.response.CustomerMessageResponse;
import com.aicarrental.api.customer.response.CustomerRegistrationResponse;
import com.aicarrental.application.customer.CustomerAuthService;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.RefreshTokenCookieService;
import com.aicarrental.infrastructure.security.RefreshTokenService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/customer/auth")
@RequiredArgsConstructor
public class CustomerAuthController {
    private final CustomerAuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final CustomerAccountRepository customerAccountRepository;

    @PostMapping("/register")
    public ResponseEntity<CustomerRegistrationResponse> register(
            @Valid @RequestBody CustomerRegisterRequest request,
            HttpServletRequest servletRequest
    ) {
        CustomerRegistrationResponse response = authService.register(request, clientIp(servletRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerAuthResponse> login(@Valid @RequestBody CustomerLoginRequest request) {
        CustomerAuthResponse response = authService.login(request);
        String refreshToken = refreshTokenService.issueToken(
                RefreshTokenPrincipalType.CUSTOMER,
                response.customerId()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshCookie(refreshToken).toString())
                .body(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<CustomerMessageResponse> verifyEmail(@Valid @RequestBody CustomerTokenRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.token()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<CustomerMessageResponse> resendVerification(
            @Valid @RequestBody CustomerEmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(authService.resendVerification(request.email(), clientIp(servletRequest)));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<CustomerMessageResponse> forgotPassword(
            @Valid @RequestBody CustomerEmailRequest request,
            HttpServletRequest servletRequest
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request.email(), clientIp(servletRequest)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CustomerMessageResponse> resetPassword(
            @Valid @RequestBody CustomerResetPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.resetPassword(request.token(), request.newPassword()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CustomerAuthResponse> refresh(
            @CookieValue(name = RefreshTokenService.CUSTOMER_COOKIE_NAME, required = false) String rawRefreshToken
    ) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken,
                RefreshTokenPrincipalType.CUSTOMER
        );
        CustomerAccount customer = customerAccountRepository
                .findById(rotation.principalId())
                .filter(account -> Boolean.TRUE.equals(account.getActive()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session is invalid"));
        CustomerAuthResponse response = authService.createAuthResponse(customer);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshCookie(rotation.rawToken()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenService.CUSTOMER_COOKIE_NAME, required = false) String rawRefreshToken
    ) {
        refreshTokenService.revoke(rawRefreshToken, RefreshTokenPrincipalType.CUSTOMER);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return refreshTokenCookieService.createCookie(
                RefreshTokenService.CUSTOMER_COOKIE_NAME,
                refreshToken,
                refreshTokenService.getRefreshExpirationSeconds(),
                "/api/customer/auth"
        );
    }

    private ResponseCookie clearRefreshCookie() {
        return refreshTokenCookieService.clearCookie(
                RefreshTokenService.CUSTOMER_COOKIE_NAME,
                "/api/customer/auth"
        );
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
