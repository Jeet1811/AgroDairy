package com.agrodairy.auth.controller;

import com.agrodairy.auth.dto.AuthResponse;
import com.agrodairy.auth.dto.CreateStaffRequest;
import com.agrodairy.auth.dto.CreateStaffResponse;
import com.agrodairy.auth.dto.LoginRequest;
import com.agrodairy.auth.dto.RefreshRequest;
import com.agrodairy.auth.dto.RegisterRequest;
import com.agrodairy.auth.dto.TokenResponse;
import com.agrodairy.auth.dto.UserResponse;
import com.agrodairy.auth.security.AuthenticatedUser;
import com.agrodairy.auth.service.AuthService;
import com.agrodairy.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal AuthenticatedUser principal) {
        UserResponse response = authService.me(principal.id());
        return ResponseEntity.ok(ApiResponse.of(response));
    }

    @PostMapping("/staff")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreateStaffResponse>> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        CreateStaffResponse response = authService.createStaff(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }
}
