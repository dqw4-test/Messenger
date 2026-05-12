package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.GroupMember;
import com.example.socialnetwork.domain.model.GroupRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(Long groupId);

    List<GroupMember> findByGroupIdAndRole(Long groupId, GroupRole role);

    boolean existsByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupIdAndUserId(Long groupId, Long userId);

    void deleteByGroupId(Long groupId);

    @Query("""
            select gm from GroupMember gm
            where gm.user.id = :userId
              and (lower(gm.group.title) like lower(concat('%', :query, '%'))
                   or str(gm.group.id) like concat('%', :query, '%'))
            order by gm.group.id desc
            """)
    List<GroupMember> searchMyGroups(@Param("userId") Long userId, @Param("query") String query);
}
