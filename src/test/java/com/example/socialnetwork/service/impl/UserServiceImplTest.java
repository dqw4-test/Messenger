package com.example.socialnetwork.service.impl;

import com.example.socialnetwork.domain.dto.UserDto;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.mapper.UserMapper;
import com.example.socialnetwork.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUsersWithTxShouldSaveAll() {
        UserDto firstDto = new UserDto(null, "john", "john@mail.local", null);
        UserDto secondDto = new UserDto(null, "kate", "kate@mail.local", null);

        when(userMapper.toEntity(any(UserDto.class))).thenAnswer(invocation -> {
            UserDto dto = invocation.getArgument(0);
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        });
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(userMapper.toDto(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            return new UserDto(user.getId(), user.getUsername(), user.getEmail(), "2026-04-18T10:00:00");
        });

        List<UserDto> result = userService.createUsersWithTx(List.of(firstDto, secondDto));

        assertEquals(2, result.size());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void createUsersWithoutTxShouldKeepPreviousUsersWhenNextIsInvalid() {
        UserDto valid = new UserDto(null, "john", "john@mail.local", null);
        UserDto invalid = new UserDto(null, "kate", "invalid-email", null);

        when(userMapper.toEntity(any(UserDto.class))).thenAnswer(invocation -> {
            UserDto dto = invocation.getArgument(0);
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        });
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUsersWithoutTx(List.of(valid, invalid))
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("email format is invalid", exception.getReason());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void createUsersWithTxShouldThrowWhenInvalidUserAppearsInTheMiddle() {
        UserDto valid = new UserDto(null, "john", "john@mail.local", null);
        UserDto invalid = new UserDto(null, "bad name", "kate@mail.local", null);

        when(userMapper.toEntity(any(UserDto.class))).thenAnswer(invocation -> {
            UserDto dto = invocation.getArgument(0);
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        });
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUsersWithTx(List.of(valid, invalid))
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void getUserByIdShouldReturnMappedDtoWhenExists() {
        User user = new User();
        user.setId(1L);
        user.setUsername("john");
        user.setEmail("john@mail.local");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        UserDto expected = new UserDto(1L, "john", "john@mail.local", "2026-04-18T10:00:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toDto(user)).thenReturn(expected);

        UserDto result = userService.getUserById(1L);

        assertEquals(expected.getId(), result.getId());
        assertEquals(expected.getUsername(), result.getUsername());
        assertEquals(expected.getEmail(), result.getEmail());
    }

    @Test
    void createUsersWithoutTxShouldThrowWhenPrefixPayloadHasDuplicateUsername() {
        UserDto first = new UserDto(null, "john", "john@mail.local", null);
        UserDto second = new UserDto(null, "john", "john2@mail.local", null);

        when(userMapper.toEntity(any(UserDto.class))).thenAnswer(invocation -> {
            UserDto dto = invocation.getArgument(0);
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setEmail(dto.getEmail());
            return user;
        });
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUsersWithoutTx(List.of(first, second))
        );

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("Duplicate username in payload: john", exception.getReason());
        verify(userRepository, times(1)).save(any(User.class));
    }
}
