package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.message.MessageDeleteRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageDto;
import com.example.socialnetwork.domain.dto.message.MessageEditRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageSendRequestDto;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Messages API")
public class MessagesController {
    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/send")
    @Operation(summary = "Send message to direct chat or group")
    public MessageDto send(@Valid @RequestBody MessageSendRequestDto request) {
        return chatService.send(currentUserProvider.getCurrentUserId(), request);
    }

    @PatchMapping("/edit")
    @Operation(summary = "Edit a message")
    public MessageDto edit(@Valid @RequestBody MessageEditRequestDto request) {
        return chatService.edit(currentUserProvider.getCurrentUserId(), request);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete a message")
    public void delete(@Valid @RequestBody MessageDeleteRequestDto request) {
        chatService.deleteMessage(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/history")
    @Operation(summary = "Get message history")
    public OffsetPageResponse<MessageDto> getHistory(@RequestParam(name = "chat_id", required = false) Long chatId,
                                                     @RequestParam(name = "group_id", required = false) Long groupId,
                                                     @RequestParam(required = false) Integer limit,
                                                     @RequestParam(required = false) Integer offset,
                                                     @RequestParam(required = false) Integer page) {
        return chatService.getHistory(currentUserProvider.getCurrentUserId(), chatId, groupId, limit, offset, page);
    }
}
