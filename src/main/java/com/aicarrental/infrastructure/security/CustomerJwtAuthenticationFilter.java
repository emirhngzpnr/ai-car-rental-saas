package com.aicarrental.infrastructure.security;

import com.aicarrental.infrastructure.persistence.CustomerAccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CustomerJwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final CustomerAccountRepository customerAccountRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/customer/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        if (!jwtService.isTokenValid(token) || !"CUSTOMER".equals(jwtService.extractPrincipalType(token))) {
            filterChain.doFilter(request, response);
            return;
        }

        customerAccountRepository.findByEmailIgnoreCaseAndActiveTrue(jwtService.extractEmail(token))
                .filter(account -> Boolean.TRUE.equals(account.getEmailVerified()))
                .filter(account -> SecurityContextHolder.getContext().getAuthentication() == null)
                .ifPresent(account -> SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                account,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
                        )
                ));

        filterChain.doFilter(request, response);
    }
}

