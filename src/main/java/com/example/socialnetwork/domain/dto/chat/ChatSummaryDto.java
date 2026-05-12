package com.example.socialnetwork.domain.dto.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatSummaryDto {
    private Long id;
    @JsonProperty("peer_id")
    private Long peerId;
    @JsonProperty("peer_name")
    private String peerName;
    @JsonProperty("last_message")
    private String lastMessage;
}
