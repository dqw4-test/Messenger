package com.example.socialnetwork.service.group;

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
import com.example.socialnetwork.domain.dto.paging.OffsetPageQuery;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.domain.model.GroupChat;
import com.example.socialnetwork.domain.model.GroupJoinRequest;
import com.example.socialnetwork.domain.model.GroupMember;
import com.example.socialnetwork.domain.model.GroupRole;
import com.example.socialnetwork.domain.model.Message;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.GroupChatRepository;
import com.example.socialnetwork.repository.GroupJoinRequestRepository;
import com.example.socialnetwork.repository.GroupMemberRepository;
import com.example.socialnetwork.repository.MessageAttachmentRepository;
import com.example.socialnetwork.repository.MessageRepository;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.service.account.AccountPrivacyService;
import com.example.socialnetwork.service.paging.PageableQueryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {
    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UserRepository userRepository;
    private final AccountPrivacyService accountPrivacyService;
    private final PageableQueryService pageableQueryService;

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryDto> getGroups(Long currentUserId) {
        return groupChatRepository.findAllByMember(currentUserId).stream()
                .map(group -> new GroupSummaryDto(group.getId(), group.getTitle()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryDto> getOwnedGroups(Long currentUserId) {
        return groupChatRepository.findAllByOwnerIdOrderByIdDesc(currentUserId).stream()
                .map(group -> new GroupSummaryDto(group.getId(), group.getTitle()))
                .toList();
    }

    @Override
    @Transactional
    public GroupSummaryDto create(Long currentUserId, GroupCreateRequestDto request) {
        User owner = getUser(currentUserId);
        String title = normalizeTitle(request.getTitle());
        GroupChat group = new GroupChat();
        group.setTitle(title);
        group.setOwner(owner);
        group.setMembersVisible(true);
        group.setCanUsersInvite(true);
        GroupChat saved = groupChatRepository.save(group);
        groupMemberRepository.save(new GroupMember(null, saved, owner, GroupRole.ADMIN, null));
        return new GroupSummaryDto(saved.getId(), saved.getTitle());
    }

    @Override
    @Transactional
    public void deleteGroup(Long currentUserId, GroupDeleteRequestDto request) {
        if (request.getGroupId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "group_id is required");
        }
        GroupChat group = getGroup(request.getGroupId());
        requireAdmin(group.getId(), currentUserId);
        deleteGroupCompletely(group);
    }

    @Override
    @Transactional
    public List<Long> invite(Long currentUserId, GroupInviteRequestDto request) {
        GroupChat group = getGroup(request.getGroupId());
        GroupMember inviterMembership = requireMember(group.getId(), currentUserId);
        if (!group.isCanUsersInvite() && inviterMembership.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot invite users to this group");
        }

        List<Long> failedUserIds = new ArrayList<>();
        for (Long userId : request.getUserIds()) {
            if (userId == null || userId <= 0) {
                failedUserIds.add(userId);
                continue;
            }
            if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
                continue;
            }
            boolean hasPendingApply = groupJoinRequestRepository.findByGroupIdAndApplicantId(group.getId(), userId).isPresent();
            if (!hasPendingApply && !accountPrivacyService.canInvite(userId, currentUserId)) {
                failedUserIds.add(userId);
                continue;
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                failedUserIds.add(userId);
                continue;
            }
            groupMemberRepository.save(new GroupMember(null, group, user, GroupRole.MEMBER, null));
            groupJoinRequestRepository.deleteByGroupIdAndApplicantId(group.getId(), userId);
        }
        return failedUserIds;
    }

    @Override
    @Transactional
    public void leave(Long currentUserId, GroupLeaveRequestDto request) {
        GroupChat group = getGroup(request.getGroupId());
        removeMember(group, currentUserId, Boolean.TRUE.equals(request.getDeleteMessages()));
    }

    @Override
    @Transactional
    public void kick(Long currentUserId, GroupKickRequestDto request) {
        GroupChat group = getGroup(request.getGroupId());
        requireAdmin(group.getId(), currentUserId);
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "user_id is required");
        }
        removeMember(group, request.getUserId(), Boolean.TRUE.equals(request.getDeleteMessages()));
    }

    @Override
    @Transactional
    public void setAdmins(Long currentUserId, GroupSetAdminsRequestDto request) {
        requireAdmin(request.getGroupId(), currentUserId);
        for (Long userId : request.getUserIds()) {
            GroupMember member = requireMember(request.getGroupId(), userId);
            member.setRole(GroupRole.ADMIN);
            groupMemberRepository.save(member);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<Long> getMembers(Long currentUserId, Long groupId, Integer limit, Integer offset, Integer page) {
        GroupChat group = getGroup(groupId);
        GroupMember requester = requireMember(groupId, currentUserId);
        if (!group.isMembersVisible() && requester.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Group members are hidden");
        }
        List<Long> members = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(groupId).stream()
                .map(member -> member.getUser().getId())
                .toList();
        OffsetPageQuery query = pageableQueryService.resolve(limit, offset, page);
        return pageableQueryService.build(members, query);
    }

    @Override
    @Transactional
    public GroupPrivacyDto setPrivacy(Long currentUserId, GroupPrivacyUpdateRequestDto request) {
        GroupChat group = requireAdmin(request.getGroupId(), currentUserId).getGroup();
        if (request.getMembersVisible() != null) {
            group.setMembersVisible(request.getMembersVisible());
        }
        if (request.getCanUsersInvite() != null) {
            group.setCanUsersInvite(request.getCanUsersInvite());
        }
        GroupChat saved = groupChatRepository.save(group);
        return new GroupPrivacyDto(saved.isMembersVisible(), saved.isCanUsersInvite());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupPrivacyDto getPrivacy(Long currentUserId, Long groupId) {
        GroupChat group = requireAdmin(groupId, currentUserId).getGroup();
        return new GroupPrivacyDto(group.isMembersVisible(), group.isCanUsersInvite());
    }

    @Override
    @Transactional
    public void apply(Long currentUserId, GroupApplyRequestDto request) {
        GroupChat group = getGroup(request.getGroupId());
        if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), currentUserId)) {
            return;
        }
        if (groupJoinRequestRepository.findByGroupIdAndApplicantId(group.getId(), currentUserId).isPresent()) {
            return;
        }
        GroupJoinRequest joinRequest = new GroupJoinRequest();
        joinRequest.setGroup(group);
        joinRequest.setApplicant(getUser(currentUserId));
        groupJoinRequestRepository.save(joinRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getInvites(Long currentUserId, Long groupId) {
        requireAdmin(groupId, currentUserId);
        return groupJoinRequestRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .map(request -> request.getApplicant().getId())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupSummaryDto> searchMyGroups(Long currentUserId, String query) {
        String normalized = query == null ? "" : query.trim();
        return groupMemberRepository.searchMyGroups(currentUserId, normalized).stream()
                .map(GroupMember::getGroup)
                .distinct()
                .map(group -> new GroupSummaryDto(group.getId(), group.getTitle()))
                .toList();
    }

    private GroupMember requireAdmin(Long groupId, Long currentUserId) {
        GroupMember member = requireMember(groupId, currentUserId);
        if (member.getRole() != GroupRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not a group admin");
        }
        return member;
    }

    private GroupMember requireMember(Long groupId, Long userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not a group member"));
    }

    private GroupChat getGroup(Long groupId) {
        return groupChatRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private String normalizeTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Group title is required");
        }
        return title.trim();
    }

    private void deleteGroupMessagesByAuthor(Long groupId, Long authorId) {
        List<Message> messages = messageRepository.findByGroupOrder(groupId).stream()
                .filter(message -> message.getFromUser().getId().equals(authorId))
                .toList();
        messages.forEach(message -> {
            messageAttachmentRepository.deleteByMessageId(message.getId());
            messageRepository.delete(message);
        });
    }

    private void removeMember(GroupChat group, Long userId, boolean deleteMessages) {
        GroupMember leaving = requireMember(group.getId(), userId);
        boolean leavingIsOwner = group.getOwner() != null && group.getOwner().getId().equals(userId);
        if (deleteMessages) {
            deleteGroupMessagesByAuthor(group.getId(), userId);
        }
        groupMemberRepository.deleteByGroupIdAndUserId(group.getId(), userId);
        List<GroupMember> remainingMembers = groupMemberRepository.findByGroupIdOrderByJoinedAtAsc(group.getId());
        if (remainingMembers.isEmpty()) {
            deleteGroupCompletely(group);
            return;
        }
        if (leaving.getRole() == GroupRole.ADMIN) {
            List<GroupMember> admins = groupMemberRepository.findByGroupIdAndRole(group.getId(), GroupRole.ADMIN);
            if (admins.isEmpty()) {
                remainingMembers.stream()
                        .findFirst()
                        .ifPresent(next -> {
                            next.setRole(GroupRole.ADMIN);
                            groupMemberRepository.save(next);
                        });
            }
        }
        if (leavingIsOwner) {
            GroupMember nextOwnerMember = remainingMembers.get(0);
            group.setOwner(nextOwnerMember.getUser());
            groupChatRepository.save(group);
        }
    }

    private void deleteGroupCompletely(GroupChat group) {
        messageRepository.findByGroupOrder(group.getId()).forEach(message -> {
            messageAttachmentRepository.deleteByMessageId(message.getId());
            messageRepository.delete(message);
        });
        groupJoinRequestRepository.deleteByGroupId(group.getId());
        groupMemberRepository.deleteByGroupId(group.getId());
        groupChatRepository.delete(group);
    }
}
