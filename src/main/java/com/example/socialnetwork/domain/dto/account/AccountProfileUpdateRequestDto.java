package com.example.socialnetwork.domain.dto.account;

import com.example.socialnetwork.domain.model.UserGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Data;

@Data
@Schema(description = "Partial update for current user profile")
public class AccountProfileUpdateRequestDto {
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 1000)
    private String about;

    @Past
    private LocalDate birthDate;

    @Size(max = 120)
    private String city;

    private UserGender gender;
}
