package com.example.socialnetwork.mapper;

import com.example.socialnetwork.domain.dto.UserDto;
import com.example.socialnetwork.domain.model.User;
import org.springframework.stereotype.Component;
import java.time.format.DateTimeFormatter;

@Component
public class UserMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt().format(FORMATTER) : null);
        return dto;
    }

    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        return user;
    }
}