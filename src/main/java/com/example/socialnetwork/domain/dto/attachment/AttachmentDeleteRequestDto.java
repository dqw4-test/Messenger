package com.example.socialnetwork.domain.dto.attachment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Delete attachment request")
public class AttachmentDeleteRequestDto {
    @NotBlank
    @Schema(example = "photo_1")
    private String attachmentKey;
}
