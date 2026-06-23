package com.aicarrental.api.customer;

import com.aicarrental.api.customer.request.CustomerLoginRequest;
import com.aicarrental.api.customer.request.CustomerRegisterRequest;
import com.aicarrental.api.customer.response.CustomerAuthResponse;
import com.aicarrental.application.customer.CustomerAuthService;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.domain.customer.CustomerAccount;
import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import com.aicarrental.infrastructure.security.RefreshTokenCookieService;
import com.aicarrental.infrastructure.security.RefreshTokenService;
import jakarta.validation.Valid;
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
    public ResponseEntity<CustomerAuthResponse> register(@Valid @RequestBody CustomerRegisterRequest request) {
        CustomerAuthResponse response = authService.register(request);
        String refreshToken = refreshTokenService.issueToken(
                RefreshTokenPrincipalType.CUSTOMER,
                response.customerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, createRefreshCookie(refreshToken).toString())
                .body(response);
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
}
