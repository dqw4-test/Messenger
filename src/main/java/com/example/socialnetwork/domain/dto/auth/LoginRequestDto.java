package com.example.socialnetwork.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request")
public class LoginRequestDto {
    @Email
    @NotBlank
    @Schema(example = "john@mail.local")
    private String email;

    @NotBlank
    @Schema(example = "StrongPassword123!")
    private String password;
}
