package com.example.socialnetwork.service;

import com.example.socialnetwork.domain.dto.UserDto;
import java.util.List;

public interface UserService {
    List<UserDto> getAllUsers();

    UserDto getUserById(Long id);

    UserDto createUser(UserDto userDto);

    List<UserDto> createUsersWithTx(List<UserDto> userDtos);

    List<UserDto> createUsersWithoutTx(List<UserDto> userDtos);

    UserDto updateUser(Long id, UserDto userData);

    void deleteUser(Long id);
}
