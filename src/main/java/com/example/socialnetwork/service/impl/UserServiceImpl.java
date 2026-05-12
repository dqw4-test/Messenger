package com.example.socialnetwork.service.impl;

import com.example.socialnetwork.domain.dto.UserDto;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.mapper.UserMapper;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.service.UserService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9._-]{3,50}$";
    private static final String EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));
    }

    @Override
    @Transactional
    public UserDto createUser(UserDto userDto) {
        User user = buildUser(userDto);
        validateUser(user, new HashSet<>(), new HashSet<>());
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public List<UserDto> createUsersWithTx(List<UserDto> userDtos) {
        return createUsersSequentially(userDtos);
    }

    @Override
    public List<UserDto> createUsersWithoutTx(List<UserDto> userDtos) {
        return createUsersSequentially(userDtos);
    }

    @Override
    @Transactional
    public UserDto updateUser(Long id, UserDto userData) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "User not found"));

        String username = normalizeRequired(userData.getUsername(), "username");
        String email = normalizeRequired(userData.getEmail(), "email");
        validateUsernameFormat(username);
        validateEmailFormat(email);

        userRepository.findByUsername(username)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(CONFLICT, "Username already exists: " + username);
                });

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(CONFLICT, "Email already exists: " + email);
                });

        user.setUsername(username);
        user.setEmail(email);
        User saved = userRepository.save(user);
        return userMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    private List<UserDto> createUsersSequentially(List<UserDto> userDtos) {
        validateBulkPayload(userDtos);

        Set<String> payloadUsernames = new HashSet<>();
        Set<String> payloadEmails = new HashSet<>();
        List<UserDto> createdUsers = new ArrayList<>();

        for (UserDto userDto : userDtos) {
            User user = buildUser(userDto);
            validateUser(user, payloadUsernames, payloadEmails);
            User saved = userRepository.save(user);
            createdUsers.add(userMapper.toDto(saved));
        }

        return createdUsers;
    }

    private void validateBulkPayload(List<UserDto> userDtos) {
        if (userDtos == null || userDtos.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Bulk payload must contain at least one user");
        }
    }

    private User buildUser(UserDto userDto) {
        if (userDto == null) {
            throw new ResponseStatusException(BAD_REQUEST, "User payload item must not be null");
        }

        String username = normalizeRequired(userDto.getUsername(), "username");
        String email = normalizeRequired(userDto.getEmail(), "email");
        validateUsernameFormat(username);
        validateEmailFormat(email);

        User user = Optional.ofNullable(userMapper.toEntity(userDto)).orElseGet(User::new);
        user.setId(null);
        user.setUsername(username);
        user.setEmail(email);
        user.setCreatedAt(LocalDateTime.now());
        return user;
    }

    private void validateUser(User user, Set<String> payloadUsernames, Set<String> payloadEmails) {
        if (!payloadUsernames.add(user.getUsername())) {
            throw new ResponseStatusException(BAD_REQUEST, "Duplicate username in payload: " + user.getUsername());
        }
        if (!payloadEmails.add(user.getEmail())) {
            throw new ResponseStatusException(BAD_REQUEST, "Duplicate email in payload: " + user.getEmail());
        }

        if (userRepository.existsByUsername(user.getUsername())) {
            throw new ResponseStatusException(CONFLICT, "Username already exists: " + user.getUsername());
        }
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new ResponseStatusException(CONFLICT, "Email already exists: " + user.getEmail());
        }
    }

    private void validateUsernameFormat(String username) {
        if (!username.matches(USERNAME_PATTERN)) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "username must match ^[a-zA-Z0-9._-]{3,50}$"
            );
        }
    }

    private void validateEmailFormat(String email) {
        if (!email.matches(EMAIL_PATTERN)) {
            throw new ResponseStatusException(BAD_REQUEST, "email format is invalid");
        }
    }

    private String normalizeRequired(String value, String fieldName) {
        return Optional.ofNullable(value)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, fieldName + " is required"));
    }
}
