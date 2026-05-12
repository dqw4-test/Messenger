package com.example.socialnetwork.service.chat;

import com.example.socialnetwork.domain.dto.chat.ChatDeleteRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatOpenRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatSummaryDto;
import com.example.socialnetwork.domain.dto.message.MessageDeleteRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageDto;
import com.example.socialnetwork.domain.dto.message.MessageEditRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageSendRequestDto;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import java.util.List;

public interface ChatService {
    MessageDto send(Long currentUserId, MessageSendRequestDto request);

    MessageDto edit(Long currentUserId, MessageEditRequestDto request);

    void deleteMessage(Long currentUserId, MessageDeleteRequestDto request);

    OffsetPageResponse<MessageDto> getHistory(Long currentUserId, Long chatId, Long groupId, Integer limit, Integer offset, Integer page);

    List<ChatSummaryDto> getChats(Long currentUserId);

    ChatSummaryDto openChat(Long currentUserId, ChatOpenRequestDto request);

    void deleteChat(Long currentUserId, ChatDeleteRequestDto request);

    List<ChatSummaryDto> searchMyChats(Long currentUserId, String query);
}
