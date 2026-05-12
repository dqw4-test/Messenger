package com.example.socialnetwork.domain.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "All account privacy settings")
public class AccountPrivacySettingsDto {
    private PrivacyRuleDto canMessageMe;
    private PrivacyRuleDto canSeeInfo;
    private PrivacyRuleDto canInviteMe;
}
