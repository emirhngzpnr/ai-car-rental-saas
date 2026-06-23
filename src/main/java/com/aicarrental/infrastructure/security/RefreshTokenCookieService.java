package com.aicarrental.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RefreshTokenCookieService {
    @Value("${app.auth.refresh-cookie.secure}")
    private boolean secure;

    @Value("${app.auth.refresh-cookie.same-site}")
    private String sameSite;

    public ResponseCookie createCookie(String name, String rawToken, long maxAgeSeconds, String path) {
        return ResponseCookie.from(name, rawToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    public ResponseCookie clearCookie(String name, String path) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }
}
