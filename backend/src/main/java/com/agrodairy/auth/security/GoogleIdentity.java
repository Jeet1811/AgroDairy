package com.agrodairy.auth.security;

public record GoogleIdentity(
        String sub,
        String email,
        boolean emailVerified,
        String name
) {}
