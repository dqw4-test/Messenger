package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.group.GroupApplyRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupCreateRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupDeleteRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupInviteRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupKickRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupLeaveRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupPrivacyDto;
import com.example.socialnetwork.domain.dto.group.GroupPrivacyUpdateRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupSetAdminsRequestDto;
import com.example.socialnetwork.domain.dto.group.GroupSummaryDto;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.group.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Groups API")
public class GroupsController {
    private final GroupService groupService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public List<GroupSummaryDto> get() {
        return groupService.getGroups(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/owned")
    public List<GroupSummaryDto> getOwned() {
        return groupService.getOwnedGroups(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/create")
    public GroupSummaryDto create(@Valid @RequestBody GroupCreateRequestDto request) {
        return groupService.create(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/delete")
    public void delete(@Valid @RequestBody GroupDeleteRequestDto request) {
        groupService.deleteGroup(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/invite")
    public List<Long> invite(@Valid @RequestBody GroupInviteRequestDto request) {
        return groupService.invite(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/leave")
    public void leave(@Valid @RequestBody GroupLeaveRequestDto request) {
        groupService.leave(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/kick")
    public void kick(@Valid @RequestBody GroupKickRequestDto request) {
        groupService.kick(currentUserProvider.getCurrentUserId(), request);
    }

    @PostMapping("/setAdmins")
    public void setAdmins(@Valid @RequestBody GroupSetAdminsRequestDto request) {
        groupService.setAdmins(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/members")
    public OffsetPageResponse<Long> getMembers(@RequestParam(name = "group_id") Long groupId,
                                               @RequestParam(required = false) Integer limit,
                                               @RequestParam(required = false) Integer offset,
                                               @RequestParam(required = false) Integer page) {
        return groupService.getMembers(currentUserProvider.getCurrentUserId(), groupId, limit, offset, page);
    }

    @PatchMapping("/privacy")
    public GroupPrivacyDto setPrivacy(@Valid @RequestBody GroupPrivacyUpdateRequestDto request) {
        return groupService.setPrivacy(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/privacy")
    public GroupPrivacyDto getPrivacy(@RequestParam(name = "group_id") Long groupId) {
        return groupService.getPrivacy(currentUserProvider.getCurrentUserId(), groupId);
    }

    @PostMapping("/apply")
    public void apply(@Valid @RequestBody GroupApplyRequestDto request) {
        groupService.apply(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/invites")
    public List<Long> getInvites(@RequestParam(name = "group_id") Long groupId) {
        return groupService.getInvites(currentUserProvider.getCurrentUserId(), groupId);
    }

    @GetMapping("/search")
    public List<GroupSummaryDto> search(@RequestParam String query) {
        return groupService.searchMyGroups(currentUserProvider.getCurrentUserId(), query);
    }
}
