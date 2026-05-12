package com.example.socialnetwork.service.attachment;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDeleteRequestDto;
import com.example.socialnetwork.domain.dto.attachment.AttachmentDto;
import com.example.socialnetwork.domain.model.Attachment;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface AttachmentService {
    List<AttachmentDto> upload(Long currentUserId, String type, List<MultipartFile> files);

    List<AttachmentDto> getCurrentUserAttachments(Long currentUserId);

    void delete(Long currentUserId, AttachmentDeleteRequestDto request);

    List<Attachment> resolveOwnedAttachments(Long currentUserId, List<String> attachmentKeys);

    AttachmentDto toDto(Attachment attachment);

    Resource loadContent(Long currentUserId, String attachmentKey);

    String contentType(Long currentUserId, String attachmentKey);
}
