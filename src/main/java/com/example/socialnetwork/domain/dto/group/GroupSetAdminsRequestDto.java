package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class GroupSetAdminsRequestDto {
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("user_ids")
    private List<Long> userIds;
}
