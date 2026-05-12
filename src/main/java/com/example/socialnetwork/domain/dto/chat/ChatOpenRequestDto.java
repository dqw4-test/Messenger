package com.example.socialnetwork.domain.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ChatOpenRequestDto {
    @JsonProperty("peer_id")
    private Long peerId;
}
