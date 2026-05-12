package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query("""
            select m from Message m
            where m.directChat.id = :chatId
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findByDirectChatOrder(@Param("chatId") Long chatId);

    @Query("""
            select m from Message m
            where m.group.id = :groupId
            order by m.createdAt desc, m.id desc
            """)
    List<Message> findByGroupOrder(@Param("groupId") Long groupId);

    @Query("""
            select m from Message m
            where m.directChat.id = :chatId
              and lower(coalesce(m.messageText, '')) like lower(concat('%', :query, '%'))
            order by m.createdAt desc
            """)
    List<Message> searchChatMessages(@Param("chatId") Long chatId, @Param("query") String query);

    void deleteByDirectChatIdAndFromUserId(Long chatId, Long fromUserId);

    void deleteByDirectChatId(Long chatId);

    void deleteByGroupIdAndFromUserId(Long groupId, Long fromUserId);

    boolean existsByDirectChatIdAndFromUserId(Long chatId, Long fromUserId);
}
