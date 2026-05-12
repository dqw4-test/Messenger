package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.account.AccountPrivacySettingsDto;
import com.example.socialnetwork.domain.dto.account.AccountPrivacyUpdateRequestDto;
import com.example.socialnetwork.domain.dto.account.AccountProfileUpdateRequestDto;
import com.example.socialnetwork.domain.dto.user.UserProfileDto;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.account.AccountPrivacyService;
import com.example.socialnetwork.service.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
@Tag(name = "Account", description = "Current user profile and privacy")
public class AccountController {
    private final AccountService accountService;
    private final AccountPrivacyService accountPrivacyService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public UserProfileDto getProfile() {
        return accountService.getCurrentProfile(currentUserProvider.getCurrentUserId());
    }

    @PatchMapping("/profile")
    @Operation(summary = "Update current user profile")
    public UserProfileDto updateProfile(@Valid @RequestBody AccountProfileUpdateRequestDto request) {
        return accountService.updateCurrentProfile(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/privacy")
    @Operation(summary = "Get current user privacy settings")
    public AccountPrivacySettingsDto getPrivacy() {
        return accountPrivacyService.getPrivacySettings(currentUserProvider.getCurrentUserId());
    }

    @PatchMapping("/privacy")
    @Operation(summary = "Partially update current user privacy settings")
    public AccountPrivacySettingsDto updatePrivacy(@Valid @RequestBody AccountPrivacyUpdateRequestDto request) {
        return accountPrivacyService.updatePrivacySettings(currentUserProvider.getCurrentUserId(), request);
    }
}
