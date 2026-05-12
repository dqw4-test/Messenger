package com.example.socialnetwork.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Validation error details for a single field")
public class ApiValidationErrorDto {
    @Schema(description = "Field name", example = "email")
    private String field;

    @Schema(description = "Validation error message", example = "must be a well-formed email address")
    private String message;

    @Schema(description = "Rejected value", example = "invalid-email")
    private Object rejectedValue;
}
