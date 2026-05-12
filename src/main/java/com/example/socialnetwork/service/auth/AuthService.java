package com.example.socialnetwork.service.auth;

import com.example.socialnetwork.domain.dto.auth.AuthTokenResponseDto;
import com.example.socialnetwork.domain.dto.auth.CurrentUserDto;
import com.example.socialnetwork.domain.dto.auth.LoginRequestDto;
import com.example.socialnetwork.domain.dto.auth.RefreshTokenRequestDto;
import com.example.socialnetwork.domain.dto.auth.RegisterRequestDto;

public interface AuthService {
    AuthTokenResponseDto register(RegisterRequestDto request);

    AuthTokenResponseDto login(LoginRequestDto request);

    AuthTokenResponseDto refresh(RefreshTokenRequestDto request);

    void logout(RefreshTokenRequestDto request);

    CurrentUserDto me();
}
