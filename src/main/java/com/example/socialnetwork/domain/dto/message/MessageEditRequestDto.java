package com.example.socialnetwork.domain.dto.message;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class MessageEditRequestDto {
    @JsonProperty("chat_id")
    private Long chatId;
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("message_id")
    private Long messageId;
    @JsonProperty("new_message")
    private String newMessage;
    @JsonProperty("new_attachments")
    private List<String> newAttachments;
}
