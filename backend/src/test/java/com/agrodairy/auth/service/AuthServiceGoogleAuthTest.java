package com.agrodairy.auth.service;

import com.agrodairy.auth.dto.GoogleAuthRequest;
import com.agrodairy.auth.entity.AuthProvider;
import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.RefreshTokenRepository;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.auth.security.GoogleIdentity;
import com.agrodairy.auth.security.GoogleTokenVerifier;
import com.agrodairy.auth.security.JwtService;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceGoogleAuthTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private GoogleTokenVerifier googleTokenVerifier;

    private AuthService newService() {
        return new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService,
                notificationService, googleTokenVerifier, 7);
    }

    @Test
    void createsANewCustomerAccountOnFirstGoogleSignIn() {
        AuthService service = newService();
        GoogleIdentity identity = new GoogleIdentity("google-sub-1", "new.user@example.com", true, "New User");
        when(googleTokenVerifier.verify("raw-id-token")).thenReturn(identity);
        when(userRepository.findByEmail("new.user@example.com")).thenReturn(Optional.empty());
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        service.googleAuth(new GoogleAuthRequest("raw-id-token"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User created = captor.getValue();
        assertThat(created.getEmail()).isEqualTo("new.user@example.com");
        assertThat(created.getFullName()).isEqualTo("New User");
        assertThat(created.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getGoogleId()).isEqualTo("google-sub-1");
        assertThat(created.getPasswordHash()).isNull();
    }

    @Test
    void linksGoogleIdToAnExistingLocalPasswordAccountWithTheSameEmail() {
        AuthService service = newService();
        User existing = User.builder()
                .id(UUID.randomUUID())
                .email("existing@example.com")
                .passwordHash("bcrypt-hash")
                .fullName("Existing User")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.LOCAL)
                .active(true)
                .build();
        GoogleIdentity identity = new GoogleIdentity("google-sub-2", "existing@example.com", true, "Existing User");
        when(googleTokenVerifier.verify("raw-id-token")).thenReturn(identity);
        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");

        service.googleAuth(new GoogleAuthRequest("raw-id-token"));

        assertThat(existing.getGoogleId()).isEqualTo("google-sub-2");
        verify(userRepository).save(existing);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsAnUnverifiedGoogleEmail() {
        AuthService service = newService();
        GoogleIdentity identity = new GoogleIdentity("google-sub-3", "unverified@example.com", false, "Unverified");
        when(googleTokenVerifier.verify("raw-id-token")).thenReturn(identity);

        assertThatThrownBy(() -> service.googleAuth(new GoogleAuthRequest("raw-id-token")))
                .isInstanceOf(ApiException.class);

        verify(userRepository, never()).findByEmail(any());
    }

    @Test
    void rejectsSignInForAnInactiveAccount() {
        AuthService service = newService();
        User inactive = User.builder()
                .id(UUID.randomUUID())
                .email("inactive@example.com")
                .fullName("Inactive User")
                .role(Role.CUSTOMER)
                .authProvider(AuthProvider.GOOGLE)
                .googleId("google-sub-4")
                .active(false)
                .build();
        GoogleIdentity identity = new GoogleIdentity("google-sub-4", "inactive@example.com", true, "Inactive User");
        when(googleTokenVerifier.verify("raw-id-token")).thenReturn(identity);
        when(userRepository.findByEmail("inactive@example.com")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.googleAuth(new GoogleAuthRequest("raw-id-token")))
                .isInstanceOf(ApiException.class);

        verify(jwtService, never()).generateAccessToken(any(), any(), any());
    }
}
