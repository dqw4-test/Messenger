package com.example.socialnetwork.domain.dto.account;

import com.example.socialnetwork.domain.model.PrivacyPolicyMode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Privacy rule with mode and selected user ids")
public class PrivacyRuleDto {
    @NotNull
    private PrivacyPolicyMode mode;
    private List<Long> userIds;
}
