package com.example.socialnetwork.domain.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class MessageSendRequestDto {
    @JsonProperty("chat_id")
    private Long chatId;
    @JsonProperty("group_id")
    private Long groupId;
    private String message;
    private List<String> attachments;
}
