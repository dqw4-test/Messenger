package com.example.socialnetwork.service.account;

import com.example.socialnetwork.domain.model.PrivacyPolicyMode;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PrivacyEvaluatorService {

    public boolean isAllowed(Long ownerUserId, Long requesterUserId, PrivacyPolicyMode mode, Set<Long> selectedUserIds) {
        if (requesterUserId != null && requesterUserId.equals(ownerUserId)) {
            return true;
        }
        if (requesterUserId == null) {
            return mode == PrivacyPolicyMode.ALL_EXCEPT_SELECTED && selectedUserIds.isEmpty();
        }

        return switch (mode) {
            case NOBODY -> false;
            case ONLY_SELECTED -> selectedUserIds.contains(requesterUserId);
            case ALL_EXCEPT_SELECTED -> !selectedUserIds.contains(requesterUserId);
        };
    }
}
