package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GroupPrivacyUpdateRequestDto {
    @JsonProperty("group_id")
    private Long groupId;
    @JsonProperty("membersVisible")
    private Boolean membersVisible;
    @JsonProperty("canUsersInvite")
    private Boolean canUsersInvite;
}
