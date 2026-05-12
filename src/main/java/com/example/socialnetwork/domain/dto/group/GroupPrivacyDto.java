package com.example.socialnetwork.domain.dto.group;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GroupPrivacyDto {
    @JsonProperty("membersVisible")
    private boolean membersVisible;
    @JsonProperty("canUsersInvite")
    private boolean canUsersInvite;
}
