package com.example.socialnetwork.domain.dto.attachment;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Attachment object")
public class AttachmentDto {
    private Long id;
    private String type;

    @JsonProperty("attachment_key")
    private String attachmentKey;

    @JsonProperty("file_name")
    private String fileName;

    @JsonProperty("content_type")
    private String contentType;

    @JsonProperty("size_bytes")
    private long sizeBytes;

    private Integer width;
    private Integer height;

    @JsonProperty("preview_url")
    private String previewUrl;
}
