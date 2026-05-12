package com.example.socialnetwork.service.attachment;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDeleteRequestDto;
import com.example.socialnetwork.domain.dto.attachment.AttachmentDto;
import com.example.socialnetwork.domain.model.Attachment;
import com.example.socialnetwork.domain.model.AttachmentType;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.AttachmentRepository;
import com.example.socialnetwork.repository.UserRepository;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository attachmentRepository;
    private final UserRepository userRepository;
    private final AttachmentFileStorageService attachmentFileStorageService;
    private final com.example.socialnetwork.repository.MessageAttachmentRepository messageAttachmentRepository;

    @Override
    @Transactional
    public List<AttachmentDto> upload(Long currentUserId, String type, List<MultipartFile> files) {
        AttachmentType attachmentType = parseType(type);
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        List<AttachmentDto> result = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file must not be empty");
            }
            if (attachmentType == AttachmentType.PHOTO) {
                result.add(storePhoto(owner, file));
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attachment type");
            }
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttachmentDto> getCurrentUserAttachments(Long currentUserId) {
        return attachmentRepository.findByOwnerIdOrderByCreatedAtDesc(currentUserId).stream()
                .map(attachment -> toDto(attachment, null, null))
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long currentUserId, AttachmentDeleteRequestDto request) {
        ParsedAttachmentKey parsed = parseAttachmentKey(request.getAttachmentKey());
        Attachment attachment = attachmentRepository.findByIdAndType(parsed.id(), parsed.type())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (!attachment.getOwner().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attachment does not belong to current user");
        }
        try {
            attachmentFileStorageService.delete(attachment.getStorageKey());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete attachment file");
        }
        attachmentRepository.delete(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attachment> resolveOwnedAttachments(Long currentUserId, List<String> attachmentKeys) {
        if (attachmentKeys == null || attachmentKeys.isEmpty()) {
            return List.of();
        }

        List<Attachment> attachments = new ArrayList<>();
        for (String key : attachmentKeys) {
            ParsedAttachmentKey parsed = parseAttachmentKey(key);
            Attachment attachment = attachmentRepository.findByIdAndType(parsed.id(), parsed.type())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found: " + key));
            if (!attachment.getOwner().getId().equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attachment does not belong to current user");
            }
            attachments.add(attachment);
        }
        return attachments;
    }

    @Override
    public AttachmentDto toDto(Attachment attachment) {
        return toDto(attachment, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource loadContent(Long currentUserId, String attachmentKey) {
        Attachment attachment = resolveReadableAttachment(currentUserId, attachmentKey);
        try {
            return new InputStreamResource(attachmentFileStorageService.open(attachment.getStorageKey()));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to open attachment");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String contentType(Long currentUserId, String attachmentKey) {
        return resolveReadableAttachment(currentUserId, attachmentKey).getContentType();
    }

    private AttachmentDto storePhoto(User owner, MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo upload requires image/* content type");
        }

        try {
            byte[] bytes = file.getBytes();
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid image file");
            }
            String extension = extensionOf(file.getOriginalFilename());
            String storageKey = attachmentFileStorageService.store(extension, new java.io.ByteArrayInputStream(bytes));

            Attachment attachment = new Attachment();
            attachment.setOwner(owner);
            attachment.setType(AttachmentType.PHOTO);
            attachment.setStorageKey(storageKey);
            attachment.setOriginalFilename(file.getOriginalFilename() == null ? "photo" : file.getOriginalFilename());
            attachment.setContentType(contentType);
            attachment.setSizeBytes(file.getSize());
            attachment.setImageWidth(image.getWidth());
            attachment.setImageHeight(image.getHeight());
            Attachment saved = attachmentRepository.save(attachment);
            return toDto(saved, image.getWidth(), image.getHeight());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store attachment");
        }
    }

    private AttachmentType parseType(String type) {
        if (type == null || type.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment type is required");
        }
        if ("photo".equalsIgnoreCase(type)) {
            return AttachmentType.PHOTO;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported attachment type: " + type);
    }

    private ParsedAttachmentKey parseAttachmentKey(String key) {
        String[] parts = key.split("_", 2);
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "attachmentKey must look like photo_123");
        }
        AttachmentType type = parseType(parts[0]);
        try {
            return new ParsedAttachmentKey(type, Long.parseLong(parts[1]));
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attachment id is invalid");
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1);
    }

    private AttachmentDto toDto(Attachment attachment, Integer width, Integer height) {
        String attachmentKey = attachment.getType().name().toLowerCase() + "_" + attachment.getId();
        return new AttachmentDto(
                attachment.getId(),
                attachment.getType().name().toLowerCase(),
                attachmentKey,
                attachment.getOriginalFilename(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                width != null ? width : attachment.getImageWidth(),
                height != null ? height : attachment.getImageHeight(),
                "/api/attachments/content/" + attachmentKey
        );
    }

    private Attachment resolveReadableAttachment(Long currentUserId, String attachmentKey) {
        ParsedAttachmentKey parsed = parseAttachmentKey(attachmentKey);
        Attachment attachment = attachmentRepository.findByIdAndType(parsed.id(), parsed.type())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Attachment not found"));
        if (attachment.getOwner().getId().equals(currentUserId)
                || messageAttachmentRepository.existsAccessibleByAttachmentIdAndUserId(attachment.getId(), currentUserId)) {
            return attachment;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Attachment is not accessible");
    }

    private record ParsedAttachmentKey(AttachmentType type, Long id) {
    }
}
