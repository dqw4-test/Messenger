package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.DirectChat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DirectChatRepository extends JpaRepository<DirectChat, Long> {
    @Query("""
            select c from DirectChat c
            where (c.user1.id = :first and c.user2.id = :second)
               or (c.user1.id = :second and c.user2.id = :first)
            """)
    Optional<DirectChat> findBetweenUsers(@Param("first") Long first, @Param("second") Long second);

    @Query("""
            select c from DirectChat c
            where c.user1.id = :userId or c.user2.id = :userId
            order by c.id desc
            """)
    List<DirectChat> findAllByParticipant(@Param("userId") Long userId);
}
