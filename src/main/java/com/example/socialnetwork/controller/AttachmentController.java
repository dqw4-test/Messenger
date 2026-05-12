package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDeleteRequestDto;
import com.example.socialnetwork.domain.dto.attachment.AttachmentDto;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.attachment.AttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
@Tag(name = "Attachments", description = "Attachment upload and management")
public class AttachmentController {
    private final AttachmentService attachmentService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload attachments in bulk, currently only type=photo is supported")
    public List<AttachmentDto> upload(@RequestParam String type,
                                      @RequestParam("files") List<MultipartFile> files) {
        return attachmentService.upload(currentUserProvider.getCurrentUserId(), type, files);
    }

    @GetMapping
    @Operation(summary = "Get current user attachments")
    public List<AttachmentDto> getCurrentUserAttachments() {
        return attachmentService.getCurrentUserAttachments(currentUserProvider.getCurrentUserId());
    }

    @GetMapping("/content/{attachmentKey}")
    @Operation(summary = "Get attachment binary content")
    public ResponseEntity<Resource> getContent(@PathVariable String attachmentKey) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        Resource resource = attachmentService.loadContent(currentUserId, attachmentKey);
        String contentType = attachmentService.contentType(currentUserId, attachmentKey);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=300")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @DeleteMapping
    @Operation(summary = "Delete attachment by attachment key, for example photo_1")
    public void delete(@Valid @RequestBody AttachmentDeleteRequestDto request) {
        attachmentService.delete(currentUserProvider.getCurrentUserId(), request);
    }
}
