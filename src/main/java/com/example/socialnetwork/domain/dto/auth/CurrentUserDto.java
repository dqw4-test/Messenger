package com.example.socialnetwork.domain.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import com.example.socialnetwork.domain.model.UserGender;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Current authenticated user")
public class CurrentUserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String about;
    private LocalDate birthDate;
    private String city;
    private UserGender gender;
}
