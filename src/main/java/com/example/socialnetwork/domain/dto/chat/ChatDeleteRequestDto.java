package com.example.socialnetwork.domain.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatDeleteRequestDto {
    @JsonProperty("chat_id")
    private Long chatId;
    private Boolean mine = true;
    private Boolean yours = true;
}
