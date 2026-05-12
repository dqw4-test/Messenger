package com.example.socialnetwork.service.account;

import com.example.socialnetwork.domain.dto.account.AccountPrivacySettingsDto;
import com.example.socialnetwork.domain.dto.account.AccountPrivacyUpdateRequestDto;
import com.example.socialnetwork.domain.dto.account.PrivacyRuleDto;
import com.example.socialnetwork.domain.model.AccountPrivacySettings;
import com.example.socialnetwork.domain.model.PrivacyPolicyMode;
import com.example.socialnetwork.domain.model.PrivacyRuleEntry;
import com.example.socialnetwork.domain.model.PrivacyType;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.AccountPrivacySettingsRepository;
import com.example.socialnetwork.repository.DirectChatRepository;
import com.example.socialnetwork.repository.MessageRepository;
import com.example.socialnetwork.repository.PrivacyRuleEntryRepository;
import com.example.socialnetwork.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountPrivacyService {
    private final AccountPrivacySettingsRepository accountPrivacySettingsRepository;
    private final PrivacyRuleEntryRepository privacyRuleEntryRepository;
    private final UserRepository userRepository;
    private final PrivacyEvaluatorService privacyEvaluatorService;
    private final DirectChatRepository directChatRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public AccountPrivacySettingsDto getPrivacySettings(Long currentUserId) {
        AccountPrivacySettings settings = getOrCreateSettings(currentUserId);
        return toDto(settings);
    }

    @Transactional
    public AccountPrivacySettingsDto updatePrivacySettings(Long currentUserId, AccountPrivacyUpdateRequestDto request) {
        AccountPrivacySettings settings = getOrCreateSettings(currentUserId);

        if (request.getCanMessageMe() != null) {
            applyRule(settings, PrivacyType.MESSAGE, request.getCanMessageMe());
        }
        if (request.getCanSeeInfo() != null) {
            applyRule(settings, PrivacyType.INFO, request.getCanSeeInfo());
        }
        if (request.getCanInviteMe() != null) {
            applyRule(settings, PrivacyType.INVITE, request.getCanInviteMe());
        }

        accountPrivacySettingsRepository.save(settings);
        return toDto(settings);
    }

    @Transactional(readOnly = true)
    public boolean canMessage(Long ownerUserId, Long requesterUserId) {
        if (requesterUserId == null) {
            return false;
        }
        AccountPrivacySettings settings = getExistingOrDefaultSettings(ownerUserId);
        Set<Long> selected = getSelectedUserIds(ownerUserId, PrivacyType.MESSAGE);
        boolean allowedByPolicy = privacyEvaluatorService.isAllowed(ownerUserId, requesterUserId, settings.getMessagePolicy(), selected);
        if (allowedByPolicy) {
            return true;
        }
        return directChatRepository.findBetweenUsers(ownerUserId, requesterUserId)
                .map(chat -> messageRepository.existsByDirectChatIdAndFromUserId(chat.getId(), ownerUserId))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean canSeeInfo(Long ownerUserId, Long requesterUserId) {
        AccountPrivacySettings settings = getExistingOrDefaultSettings(ownerUserId);
        Set<Long> selected = getSelectedUserIds(ownerUserId, PrivacyType.INFO);
        return privacyEvaluatorService.isAllowed(ownerUserId, requesterUserId, settings.getInfoPolicy(), selected);
    }

    @Transactional(readOnly = true)
    public boolean canInvite(Long ownerUserId, Long requesterUserId) {
        AccountPrivacySettings settings = getExistingOrDefaultSettings(ownerUserId);
        Set<Long> selected = getSelectedUserIds(ownerUserId, PrivacyType.INVITE);
        return privacyEvaluatorService.isAllowed(ownerUserId, requesterUserId, settings.getInvitePolicy(), selected);
    }

    @Transactional
    public AccountPrivacySettings ensureDefaults(User user) {
        return accountPrivacySettingsRepository.findById(user.getId())
                .orElseGet(() -> accountPrivacySettingsRepository.save(defaultSettings(user)));
    }

    private void applyRule(AccountPrivacySettings settings, PrivacyType privacyType, PrivacyRuleDto rule) {
        validateRule(rule);
        setMode(settings, privacyType, rule.getMode());

        privacyRuleEntryRepository.deleteByUserIdAndPrivacyType(settings.getUserId(), privacyType);
        for (Long targetUserId : sanitizeUserIds(rule.getUserIds())) {
            if (!userRepository.existsById(targetUserId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found in privacy rule: " + targetUserId);
            }
            PrivacyRuleEntry entry = new PrivacyRuleEntry();
            entry.setUser(settings.getUser());
            entry.setPrivacyType(privacyType);
            entry.setTargetUserId(targetUserId);
            entry.setCreatedAt(LocalDateTime.now());
            privacyRuleEntryRepository.save(entry);
        }
    }

    private AccountPrivacySettingsDto toDto(AccountPrivacySettings settings) {
        return new AccountPrivacySettingsDto(
                buildRuleDto(settings.getUserId(), settings.getMessagePolicy(), PrivacyType.MESSAGE),
                buildRuleDto(settings.getUserId(), settings.getInfoPolicy(), PrivacyType.INFO),
                buildRuleDto(settings.getUserId(), settings.getInvitePolicy(), PrivacyType.INVITE)
        );
    }

    private PrivacyRuleDto buildRuleDto(Long userId, PrivacyPolicyMode mode, PrivacyType privacyType) {
        return new PrivacyRuleDto(mode, List.copyOf(getSelectedUserIds(userId, privacyType)));
    }

    private Set<Long> getSelectedUserIds(Long userId, PrivacyType privacyType) {
        return privacyRuleEntryRepository.findByUserIdAndPrivacyType(userId, privacyType).stream()
                .map(PrivacyRuleEntry::getTargetUserId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void validateRule(PrivacyRuleDto rule) {
        if (rule.getMode() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Privacy mode is required");
        }
    }

    private Set<Long> sanitizeUserIds(List<Long> userIds) {
        if (userIds == null) {
            return Set.of();
        }
        return userIds.stream()
                .filter(id -> id != null && id > 0)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private void setMode(AccountPrivacySettings settings, PrivacyType privacyType, PrivacyPolicyMode mode) {
        switch (privacyType) {
            case MESSAGE -> settings.setMessagePolicy(mode);
            case INFO -> settings.setInfoPolicy(mode);
            case INVITE -> settings.setInvitePolicy(mode);
        }
    }

    private AccountPrivacySettings getOrCreateSettings(Long userId) {
        return accountPrivacySettingsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return accountPrivacySettingsRepository.save(defaultSettings(user));
                });
    }

    private AccountPrivacySettings getExistingOrDefaultSettings(Long userId) {
        return accountPrivacySettingsRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
                    return defaultSettings(user);
                });
    }

    private AccountPrivacySettings defaultSettings(User user) {
        AccountPrivacySettings settings = new AccountPrivacySettings();
        settings.setUser(user);
        settings.setMessagePolicy(PrivacyPolicyMode.ALL_EXCEPT_SELECTED);
        settings.setInfoPolicy(PrivacyPolicyMode.ALL_EXCEPT_SELECTED);
        settings.setInvitePolicy(PrivacyPolicyMode.ALL_EXCEPT_SELECTED);
        return settings;
    }
}
