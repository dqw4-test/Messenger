package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupDeleteRequestDto {
    @JsonProperty("group_id")
    private Long groupId;
}

