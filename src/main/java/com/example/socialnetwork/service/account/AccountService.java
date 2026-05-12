package com.example.socialnetwork.service.account;

import com.example.socialnetwork.domain.dto.account.AccountProfileUpdateRequestDto;
import com.example.socialnetwork.domain.dto.user.UserProfileDto;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.UserRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final UserRepository userRepository;
    private final AccountPrivacyService accountPrivacyService;

    @Transactional(readOnly = true)
    public UserProfileDto getCurrentProfile(Long currentUserId) {
        User user = getCurrentUser(currentUserId);
        return toOwnProfileDto(user);
    }

    @Transactional
    public UserProfileDto updateCurrentProfile(Long currentUserId, AccountProfileUpdateRequestDto request) {
        User user = getCurrentUser(currentUserId);
        if (request.getFirstName() != null) {
            user.setFirstName(normalizeRequired(request.getFirstName(), "firstName"));
        }
        if (request.getLastName() != null) {
            user.setLastName(normalizeRequired(request.getLastName(), "lastName"));
        }
        if (request.getAbout() != null) {
            user.setAbout(normalizeOptional(request.getAbout()));
        }
        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }
        if (request.getCity() != null) {
            user.setCity(normalizeOptional(request.getCity()));
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        User saved = userRepository.save(user);
        return toOwnProfileDto(saved);
    }

    private User getCurrentUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        accountPrivacyService.ensureDefaults(user);
        return user;
    }

    private UserProfileDto toOwnProfileDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                false,
                user.getAbout(),
                user.getBirthDate(),
                user.getCity(),
                user.getGender()
        );
    }

    private String normalizeRequired(String value, String fieldName) {
        String normalized = normalizeOptional(value);
        if (normalized == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must not be blank");
        }
        return normalized;
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
