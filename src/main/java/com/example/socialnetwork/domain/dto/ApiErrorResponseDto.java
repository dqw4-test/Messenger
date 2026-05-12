package com.example.socialnetwork.domain.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
@Schema(description = "Unified API error response")
public class ApiErrorResponseDto {
    @Schema(description = "Error timestamp", example = "2026-04-08T10:15:30")
    private LocalDateTime timestamp;

    @Schema(description = "HTTP status code", example = "400")
    private int status;

    @Schema(description = "HTTP status reason", example = "Bad Request")
    private String error;

    @Schema(description = "Application-level error message", example = "Validation failed")
    private String message;

    @Schema(description = "Request path", example = "/api/chats")
    private String path;

    @ArraySchema(schema = @Schema(implementation = ApiValidationErrorDto.class))
    private List<ApiValidationErrorDto> validationErrors;
}
