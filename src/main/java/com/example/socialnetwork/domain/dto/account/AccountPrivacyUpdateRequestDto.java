package com.example.socialnetwork.domain.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.Data;

@Data
@Schema(description = "Partial privacy settings update")
public class AccountPrivacyUpdateRequestDto {
    @Valid
    private PrivacyRuleDto canMessageMe;

    @Valid
    private PrivacyRuleDto canSeeInfo;

    @Valid
    private PrivacyRuleDto canInviteMe;
}
