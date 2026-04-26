package com.aicarrental.api.auth.response;

public record AuthResponse(  String token,
                             String tokenType,
                             Long userId,
                             String email,
                             String role,
                             Long tenantId) {
}
