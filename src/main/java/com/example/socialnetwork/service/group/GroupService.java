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
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import java.util.List;

public interface GroupService {
    List<GroupSummaryDto> getGroups(Long currentUserId);
    List<GroupSummaryDto> getOwnedGroups(Long currentUserId);

    GroupSummaryDto create(Long currentUserId, GroupCreateRequestDto request);

    void deleteGroup(Long currentUserId, GroupDeleteRequestDto request);

    List<Long> invite(Long currentUserId, GroupInviteRequestDto request);

    void leave(Long currentUserId, GroupLeaveRequestDto request);

    void kick(Long currentUserId, GroupKickRequestDto request);

    void setAdmins(Long currentUserId, GroupSetAdminsRequestDto request);

    OffsetPageResponse<Long> getMembers(Long currentUserId, Long groupId, Integer limit, Integer offset, Integer page);

    GroupPrivacyDto setPrivacy(Long currentUserId, GroupPrivacyUpdateRequestDto request);

    GroupPrivacyDto getPrivacy(Long currentUserId, Long groupId);

    void apply(Long currentUserId, GroupApplyRequestDto request);

    List<Long> getInvites(Long currentUserId, Long groupId);

    List<GroupSummaryDto> searchMyGroups(Long currentUserId, String query);
}
