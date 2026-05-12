package com.example.socialnetwork.controller;

import com.example.socialnetwork.domain.dto.chat.ChatDeleteRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatOpenRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatSummaryDto;
import com.example.socialnetwork.security.CurrentUserProvider;
import com.example.socialnetwork.service.chat.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
@Tag(name = "Chats", description = "Direct chats API")
public class ChatsController {
    private final ChatService chatService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "Get current user chats")
    public List<ChatSummaryDto> get() {
        return chatService.getChats(currentUserProvider.getCurrentUserId());
    }

    @PostMapping("/open")
    @Operation(summary = "Open or create a direct chat with peer")
    public ChatSummaryDto open(@Valid @RequestBody ChatOpenRequestDto request) {
        return chatService.openChat(currentUserProvider.getCurrentUserId(), request);
    }

    @DeleteMapping
    @Operation(summary = "Delete direct chat messages or whole chat")
    public void delete(@Valid @RequestBody ChatDeleteRequestDto request) {
        chatService.deleteChat(currentUserProvider.getCurrentUserId(), request);
    }

    @GetMapping("/search")
    @Operation(summary = "Search current user chats")
    public List<ChatSummaryDto> search(@RequestParam String query) {
        return chatService.searchMyChats(currentUserProvider.getCurrentUserId(), query);
    }
}
