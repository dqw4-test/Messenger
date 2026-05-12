package com.example.socialnetwork.domain.dto.message;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MessageDto {
    private Long id;
    private LocalDateTime date;
    @JsonProperty("peer_id")
    private Long peerId;
    @JsonProperty("from_id")
    private Long fromId;
    private String message;
    private List<AttachmentDto> attachments;
}
