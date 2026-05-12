package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupLeaveRequestDto {
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("delete_messages")
    private Boolean deleteMessages = false;
}
