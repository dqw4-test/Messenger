package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.GroupChat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupChatRepository extends JpaRepository<GroupChat, Long> {
    List<GroupChat> findAllByOwnerIdOrderByIdDesc(Long ownerId);

    @Query("""
            select gm.group from GroupMember gm
            where gm.user.id = :userId
            order by gm.group.id desc
            """)
    List<GroupChat> findAllByMember(@Param("userId") Long userId);

    @Query("""
            select g from GroupChat g
            where lower(g.title) like lower(concat('%', :query, '%'))
               or str(g.id) like concat('%', :query, '%')
            order by g.id desc
            """)
    List<GroupChat> searchByTitle(@Param("query") String query);
}
