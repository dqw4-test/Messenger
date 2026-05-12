package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupKickRequestDto {
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("user_id")
    private Long userId;
    @JsonProperty("delete_messages")
    private Boolean deleteMessages = false;
}

