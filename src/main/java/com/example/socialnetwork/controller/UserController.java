package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.user.UserProfileDto;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.user.UserReadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
@Tag(name = "Users", description = "Read user profiles with privacy applied")
public class UserController {
    private final UserReadService userReadService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Get all users")
    public List<UserProfileDto> getAllUsers() {
        return userReadService.getAllUsers(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by id")
    public UserProfileDto getUserById(@PathVariable @Positive Long id) {
        return userReadService.getUserById(id, currentUserProvider.getCurrentUserId());
    }
}
