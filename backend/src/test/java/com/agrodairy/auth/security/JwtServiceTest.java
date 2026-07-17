package com.agrodairy.auth.security;

import com.agrodairy.auth.entity.Role;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    @Test
    void validTokenRoundTripsClaims() {
        JwtService jwtService = new JwtService("test-secret-not-for-prod-use-only", 15);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateAccessToken(userId, "user@example.com", Role.STAFF);
        var claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("STAFF");
    }

    @Test
    void expiredTokenIsRejected() throws InterruptedException {
        // TTL of 0 minutes plus a short sleep guarantees the expiration instant is already in the past.
        JwtService jwtService = new JwtService("test-secret-not-for-prod-use-only", 0);
        String token = jwtService.generateAccessToken(UUID.randomUUID(), "user@example.com", Role.CUSTOMER);
        Thread.sleep(1000);

        assertThatThrownBy(() -> jwtService.parseClaims(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void malformedTokenIsRejected() {
        JwtService jwtService = new JwtService("test-secret-not-for-prod-use-only", 15);

        assertThatThrownBy(() -> jwtService.parseClaims("not-a-real-token"))
                .isInstanceOf(JwtException.class);
    }
}
