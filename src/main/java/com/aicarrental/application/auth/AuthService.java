package com.aicarrental.application.auth;

import com.aicarrental.api.auth.request.LoginRequest;
import com.aicarrental.api.auth.response.AuthResponse;
import com.aicarrental.domain.auth.User;
import com.aicarrental.infrastructure.persistence.UserRepository;
import com.aicarrental.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!passwordMatches) {
            throw new RuntimeException("Invalid email or password");
        }

        String token = jwtService.generateToken(user);

        Long tenantId = user.getTenant() != null ? user.getTenant().getId() : null;

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                tenantId
        );
    }
}
