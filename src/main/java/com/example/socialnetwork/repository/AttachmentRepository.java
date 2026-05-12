package com.example.socialnetwork.repository;

import com.example.socialnetwork.domain.model.Attachment;
import com.example.socialnetwork.domain.model.AttachmentType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Attachment> findByIdAndType(Long id, AttachmentType type);
}
