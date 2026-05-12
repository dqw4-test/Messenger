package com.example.socialnetwork.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "JWT access and refresh token pair")
public class AuthTokenResponseDto {
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(example = "15")
    private long accessTokenTtlMinutes;

    @Schema(example = "7")
    private long refreshTokenTtlDays;
}
