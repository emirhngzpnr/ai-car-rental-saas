package com.aicarrental.api.auth;

import com.aicarrental.api.auth.request.LoginRequest;
import com.aicarrental.api.auth.request.SetUserPasswordRequest;
import com.aicarrental.api.auth.response.AuthResponse;
import com.aicarrental.api.auth.response.AuthMessageResponse;
import com.aicarrental.application.auth.AuthService;
import com.aicarrental.application.user.UserService;
import com.aicarrental.domain.auth.RefreshTokenPrincipalType;
import com.aicarrental.domain.auth.User;
import com.aicarrental.infrastructure.persistence.UserRepository;
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
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenCookieService refreshTokenCookieService;
    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        String refreshToken = refreshTokenService.issueToken(
                RefreshTokenPrincipalType.STAFF,
                response.userId()
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshCookie(refreshToken).toString())
                .body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = RefreshTokenService.STAFF_COOKIE_NAME, required = false) String rawRefreshToken
    ) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(
                rawRefreshToken,
                RefreshTokenPrincipalType.STAFF
        );
        User user = userRepository.findByIdAndActiveTrue(rotation.principalId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh session is invalid"));
        AuthResponse response = authService.createAuthResponse(user);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, createRefreshCookie(rotation.rawToken()).toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = RefreshTokenService.STAFF_COOKIE_NAME, required = false) String rawRefreshToken
    ) {
        refreshTokenService.revoke(rawRefreshToken, RefreshTokenPrincipalType.STAFF);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/set-password")
    public ResponseEntity<AuthMessageResponse> setPassword(
            @Valid @RequestBody SetUserPasswordRequest request
    ) {
        userService.setPasswordFromInvitation(request.token(), request.newPassword());
        return ResponseEntity.ok(new AuthMessageResponse("Password has been set successfully. You can now sign in."));
    }

    private ResponseCookie createRefreshCookie(String refreshToken) {
        return refreshTokenCookieService.createCookie(
                RefreshTokenService.STAFF_COOKIE_NAME,
                refreshToken,
                refreshTokenService.getRefreshExpirationSeconds(),
                "/api/auth"
        );
    }

    private ResponseCookie clearRefreshCookie() {
        return refreshTokenCookieService.clearCookie(
                RefreshTokenService.STAFF_COOKIE_NAME,
                "/api/auth"
        );
    }
}
