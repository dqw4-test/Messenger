package com.example.socialnetwork.controller.auth;

import com.example.socialnetwork.domain.dto.auth.AuthTokenResponseDto;
import com.example.socialnetwork.domain.dto.auth.CurrentUserDto;
import com.example.socialnetwork.domain.dto.auth.LoginRequestDto;
import com.example.socialnetwork.domain.dto.auth.RefreshTokenRequestDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;
import com.example.socialnetwork.service.auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "JWT authentication flow")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a new account and issue JWT tokens")
    public AuthTokenResponseDto register(@Valid @RequestBody RegisterRequestDto request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Login and issue JWT tokens")
    public AuthTokenResponseDto login(@Valid @RequestBody LoginRequestDto request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new token pair")
    public AuthTokenResponseDto refresh(@Valid @RequestBody RefreshTokenRequestDto request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke refresh token")
    public void logout(@Valid @RequestBody RefreshTokenRequestDto request) {
        authService.logout(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user")
    public CurrentUserDto me() {
        return authService.me();
    }
}
