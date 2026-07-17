package com.agrodairy.auth.security;

import com.agrodairy.auth.entity.Role;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, Role role) {
}
