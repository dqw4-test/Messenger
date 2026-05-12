package com.example.socialnetwork.domain.dto.auth;

import com.example.socialnetwork.domain.model.UserGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
@Schema(description = "Register request")
public class RegisterRequestDto {
    @Email
    @NotBlank
    @Schema(example = "john@mail.local")
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    @Schema(example = "StrongPassword123!")
    private String password;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "John")
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Schema(example = "Doe")
    private String lastName;

    @Size(max = 1000)
    @Schema(example = "I like building distributed systems.")
    private String about;

    @Past
    @Schema(example = "1999-05-20")
    private LocalDate birthDate;

    @Size(max = 120)
    @Schema(example = "Minsk")
    private String city;

    @Schema(example = "MALE")
    private UserGender gender;
}
