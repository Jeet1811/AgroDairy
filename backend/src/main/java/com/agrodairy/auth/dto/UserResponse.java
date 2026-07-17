package com.agrodairy.auth.dto;

import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String fullName,
        Role role,
        String phone,
        boolean isActive,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getPhone(),
                user.isActive(),
                user.getCreatedAt());
    }
}
