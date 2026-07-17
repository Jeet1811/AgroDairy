package com.agrodairy.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {}
