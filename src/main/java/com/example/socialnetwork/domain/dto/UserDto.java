package com.example.socialnetwork.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User data transfer object")
public class UserDto {
    @Schema(description = "User identifier", example = "1")
    private Long id;

    @NotBlank
    @Size(min = 3, max = 50)
    @Schema(description = "Username", example = "john")
    private String username;

    @NotBlank
    @Email
    @Size(max = 100)
    @Schema(description = "Email", example = "john@mail.local")
    private String email;

    @Schema(description = "Creation timestamp", example = "2026-04-08T12:00:00")
    private String createdAt;
}
