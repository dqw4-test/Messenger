package com.example.socialnetwork.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh token request")
public class RefreshTokenRequestDto {
    @NotBlank
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
