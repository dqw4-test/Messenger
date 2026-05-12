package com.example.socialnetwork.domain.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MessageDeleteRequestDto {
    @JsonProperty("chat_id")
    private Long chatId;
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("message_id")
    private Long messageId;
}
