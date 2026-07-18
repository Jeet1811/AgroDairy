package com.agrodairy.auth.service;

import com.agrodairy.auth.dto.AuthResponse;
import com.agrodairy.auth.dto.CreateStaffRequest;
import com.agrodairy.auth.dto.CreateStaffResponse;
import com.agrodairy.auth.dto.GoogleAuthRequest;
import com.agrodairy.auth.dto.LoginRequest;
import com.agrodairy.auth.dto.RefreshRequest;
import com.agrodairy.auth.dto.RegisterRequest;
import com.agrodairy.auth.dto.TokenResponse;
import com.agrodairy.auth.dto.UserResponse;
import com.agrodairy.auth.entity.AuthProvider;
import com.agrodairy.auth.entity.RefreshToken;
import com.agrodairy.auth.entity.Role;
import com.agrodairy.auth.entity.User;
import com.agrodairy.auth.repository.RefreshTokenRepository;
import com.agrodairy.auth.repository.UserRepository;
import com.agrodairy.auth.security.GoogleIdentity;
import com.agrodairy.auth.security.GoogleTokenVerifier;
import com.agrodairy.auth.security.JwtService;
import com.agrodairy.common.exception.ApiException;
import com.agrodairy.common.exception.NotFoundException;
import com.agrodairy.common.exception.ValidationException;
import com.agrodairy.notification.entity.NotificationType;
import com.agrodairy.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Set<Role> STAFF_CREATABLE_ROLES = Set.of(Role.STAFF, Role.ADMIN);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificationService notificationService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final long refreshTokenTtlDays;

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        NotificationService notificationService,
                        GoogleTokenVerifier googleTokenVerifier,
                        @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificationService = notificationService;
        this.googleTokenVerifier = googleTokenVerifier;
        this.refreshTokenTtlDays = refreshTokenTtlDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "Email already registered");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        userRepository.saveAndFlush(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password"));
        if (!user.isActive() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid email or password");
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleAuth(GoogleAuthRequest request) {
        GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());
        if (!identity.emailVerified()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Your Google account's email is not verified");
        }

        User user = userRepository.findByEmail(identity.email()).orElse(null);
        if (user == null) {
            // First time this email has signed in via Google — create a CUSTOMER account for it,
            // same role restriction as self-registration (STAFF/ADMIN can only be created by an admin).
            user = User.builder()
                    .email(identity.email())
                    .fullName(identity.name())
                    .role(Role.CUSTOMER)
                    .authProvider(AuthProvider.GOOGLE)
                    .googleId(identity.sub())
                    .active(true)
                    .build();
            userRepository.saveAndFlush(user);
        } else {
            if (!user.isActive()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "This account is inactive");
            }
            if (user.getGoogleId() == null) {
                // Existing local-password account with the same, Google-verified email — link it
                // rather than rejecting, since Google has already proven the user owns this email.
                user.setGoogleId(identity.sub());
                userRepository.save(user);
            }
        }
        return buildAuthResponse(user);
    }

    @Transactional
    public TokenResponse refresh(RefreshRequest request) {
        RefreshToken existing = refreshTokenRepository.findByToken(sha256Hex(request.refreshToken()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid refresh token"));
        if (existing.isRevoked() || existing.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid refresh token");
        }
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        User user = existing.getUser();
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = issueRefreshToken(user);
        return new TokenResponse(accessToken, refreshToken);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByToken(sha256Hex(request.refreshToken()))
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    public UserResponse me(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return UserResponse.from(user);
    }

    @Transactional
    public CreateStaffResponse createStaff(CreateStaffRequest request) {
        if (!STAFF_CREATABLE_ROLES.contains(request.role())) {
            throw new ValidationException("role must be STAFF or ADMIN");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "CONFLICT", "Email already registered");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .role(request.role())
                .active(true)
                .build();
        userRepository.saveAndFlush(user);
        notificationService.notifyUser(user, NotificationType.WELCOME,
                "Welcome to AgroDairy AI, " + user.getFullName() + "! Your " + user.getRole() + " account is ready.");
        return new CreateStaffResponse(UserResponse.from(user));
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = issueRefreshToken(user);
        return new AuthResponse(UserResponse.from(user), accessToken, refreshToken);
    }

    private String issueRefreshToken(User user) {
        String rawToken = generateOpaqueToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(sha256Hex(rawToken))
                .expiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    private static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256Hex(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
