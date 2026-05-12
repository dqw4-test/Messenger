package com.example.socialnetwork.domain.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Compact paginated response")
public class PageResponseDto<T> {
    @ArraySchema(schema = @Schema(description = "Page content item"))
    private List<T> content;
    @Schema(description = "Total pages", example = "5")
    private int totalPages;
    @Schema(description = "Total records", example = "42")
    private long totalElements;
    @Schema(description = "Requested page size", example = "10")
    private int size;
    @Schema(description = "Current page number (0-based)", example = "0")
    private int number;
    @Schema(description = "Is first page", example = "true")
    private boolean first;
    @Schema(description = "Is last page", example = "false")
    private boolean last;
}
