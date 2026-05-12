package com.example.socialnetwork.service.user;

import com.example.socialnetwork.domain.dto.user.UserProfileDto;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.service.account.AccountPrivacyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserReadService {
    private final UserRepository userRepository;
    private final AccountPrivacyService accountPrivacyService;

    @Transactional(readOnly = true)
    public List<UserProfileDto> getAllUsers(Long requesterUserId) {
        return userRepository.findAll().stream()
                .map(user -> toUserProfileDto(user, requesterUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public UserProfileDto getUserById(Long userId, Long requesterUserId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return toUserProfileDto(user, requesterUserId);
    }

    private UserProfileDto toUserProfileDto(User user, Long requesterUserId) {
        boolean canMessage = accountPrivacyService.canMessage(user.getId(), requesterUserId);
        boolean canSeeInfo = accountPrivacyService.canSeeInfo(user.getId(), requesterUserId);
        return new UserProfileDto(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                !canMessage,
                canSeeInfo ? user.getAbout() : null,
                canSeeInfo ? user.getBirthDate() : null,
                canSeeInfo ? user.getCity() : null,
                canSeeInfo ? user.getGender() : null
        );
    }
}
