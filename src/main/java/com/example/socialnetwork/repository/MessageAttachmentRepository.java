package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.MessageAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    List<MessageAttachment> findByMessageIdOrderByPositionAsc(Long messageId);

    void deleteByMessageId(Long messageId);

    @Query(value = """
            select exists (
                select 1
                from message_attachments ma
                join messages m on m.id = ma.message_id
                left join direct_chats dc on dc.id = m.direct_chat_id
                where ma.attachment_id = :attachmentId
                  and (
                        (m.direct_chat_id is not null and (dc.user1_id = :userId or dc.user2_id = :userId))
                     or (m.group_id is not null and exists (
                            select 1
                            from group_members gm
                            where gm.group_id = m.group_id
                              and gm.user_id = :userId
                        ))
                  )
            )
            """, nativeQuery = true)
    boolean existsAccessibleByAttachmentIdAndUserId(@Param("attachmentId") Long attachmentId,
                                                    @Param("userId") Long userId);
}
