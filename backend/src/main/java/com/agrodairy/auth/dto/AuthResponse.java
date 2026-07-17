package com.agrodairy.auth.dto;

public record AuthResponse(
        UserResponse user,
        String accessToken,
        String refreshToken
) {}
