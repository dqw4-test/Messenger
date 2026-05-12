package com.example.socialnetwork.service.chat;

import com.example.socialnetwork.domain.dto.attachment.AttachmentDto;
import com.example.socialnetwork.domain.dto.chat.ChatDeleteRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatOpenRequestDto;
import com.example.socialnetwork.domain.dto.chat.ChatSummaryDto;
import com.example.socialnetwork.domain.dto.message.MessageDeleteRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageDto;
import com.example.socialnetwork.domain.dto.message.MessageEditRequestDto;
import com.example.socialnetwork.domain.dto.message.MessageSendRequestDto;
import com.example.socialnetwork.domain.dto.paging.OffsetPageQuery;
import com.example.socialnetwork.domain.dto.paging.OffsetPageResponse;
import com.example.socialnetwork.domain.model.Attachment;
import com.example.socialnetwork.domain.model.DirectChat;
import com.example.socialnetwork.domain.model.GroupChat;
import com.example.socialnetwork.domain.model.GroupMember;
import com.example.socialnetwork.domain.model.Message;
import com.example.socialnetwork.domain.model.MessageAttachment;
import com.example.socialnetwork.domain.model.User;
import com.example.socialnetwork.repository.DirectChatRepository;
import com.example.socialnetwork.repository.GroupChatRepository;
import com.example.socialnetwork.repository.GroupMemberRepository;
import com.example.socialnetwork.repository.MessageAttachmentRepository;
import com.example.socialnetwork.repository.MessageRepository;
import com.example.socialnetwork.repository.UserRepository;
import com.example.socialnetwork.service.account.AccountPrivacyService;
import com.example.socialnetwork.service.attachment.AttachmentService;
import com.example.socialnetwork.service.paging.PageableQueryService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final DirectChatRepository directChatRepository;
    private final GroupChatRepository groupChatRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final MessageRepository messageRepository;
    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UserRepository userRepository;
    private final AttachmentService attachmentService;
    private final AccountPrivacyService accountPrivacyService;
    private final PageableQueryService pageableQueryService;

    @Override
    @Transactional
    public MessageDto send(Long currentUserId, MessageSendRequestDto request) {
        ConversationTarget target = resolveTarget(currentUserId, request.getChatId(), request.getGroupId());
        String text = normalize(request.getMessage());
        List<Attachment> attachments = attachmentService.resolveOwnedAttachments(currentUserId, request.getAttachments());
        if ((text == null || text.isBlank()) && attachments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message text or attachments are required");
        }

        Message message = new Message();
        message.setDirectChat(target.directChat());
        message.setGroup(target.groupChat());
        message.setFromUser(target.currentUser());
        message.setMessageText(text);
        Message saved = messageRepository.save(message);
        saveAttachments(saved, attachments);
        return toDto(saved, target.peerId());
    }

    @Override
    @Transactional
    public MessageDto edit(Long currentUserId, MessageEditRequestDto request) {
        ConversationTarget target = resolveTarget(currentUserId, request.getChatId(), request.getGroupId());
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        validateMessageBelongs(message, target);
        if (!message.getFromUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only message author can edit it");
        }

        String text = normalize(request.getNewMessage());
        List<Attachment> attachments = attachmentService.resolveOwnedAttachments(currentUserId, request.getNewAttachments());
        if ((text == null || text.isBlank()) && attachments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Edited message requires text or attachments");
        }

        message.setMessageText(text);
        Message saved = messageRepository.save(message);
        messageAttachmentRepository.deleteByMessageId(saved.getId());
        saveAttachments(saved, attachments);
        return toDto(saved, target.peerId());
    }

    @Override
    @Transactional
    public void deleteMessage(Long currentUserId, MessageDeleteRequestDto request) {
        ConversationTarget target = resolveTarget(currentUserId, request.getChatId(), request.getGroupId());
        Message message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
        validateMessageBelongs(message, target);
        if (!message.getFromUser().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only message author can delete it");
        }
        messageAttachmentRepository.deleteByMessageId(message.getId());
        messageRepository.delete(message);
    }

    @Override
    @Transactional(readOnly = true)
    public OffsetPageResponse<MessageDto> getHistory(Long currentUserId,
                                                     Long chatId,
                                                     Long groupId,
                                                     Integer limit,
                                                     Integer offset,
                                                     Integer page) {
        ConversationTarget target = resolveTarget(currentUserId, chatId, groupId);
        List<Message> messages = target.directChat() != null
                ? messageRepository.findByDirectChatOrder(target.directChat().getId())
                : messageRepository.findByGroupOrder(target.groupChat().getId());
        OffsetPageQuery query = pageableQueryService.resolve(limit, offset, page);
        List<MessageDto> mapped = messages.stream()
                .map(message -> toDto(message, target.peerId()))
                .toList();
        return pageableQueryService.build(mapped, query);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummaryDto> getChats(Long currentUserId) {
        return directChatRepository.findAllByParticipant(currentUserId).stream()
                .map(chat -> toChatSummary(chat, currentUserId))
                .toList();
    }

    @Override
    @Transactional
    public ChatSummaryDto openChat(Long currentUserId, ChatOpenRequestDto request) {
        if (request.getPeerId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "peer_id is required");
        }
        DirectChat chat = getOrCreateChat(currentUserId, request.getPeerId());
        return toChatSummary(chat, currentUserId);
    }

    @Override
    @Transactional
    public void deleteChat(Long currentUserId, ChatDeleteRequestDto request) {
        DirectChat chat = directChatRepository.findById(request.getChatId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
        ensureParticipant(chat, currentUserId);
        Long peerId = chat.getUser1().getId().equals(currentUserId) ? chat.getUser2().getId() : chat.getUser1().getId();

        boolean mine = request.getMine() == null || request.getMine();
        boolean yours = request.getYours() == null || request.getYours();
        if (mine && yours) {
            List<Message> messages = messageRepository.findByDirectChatOrder(chat.getId());
            messages.forEach(message -> messageAttachmentRepository.deleteByMessageId(message.getId()));
            messageRepository.deleteByDirectChatId(chat.getId());
            directChatRepository.delete(chat);
            return;
        }
        if (mine) {
            List<Message> mineMessages = messageRepository.findByDirectChatOrder(chat.getId()).stream()
                    .filter(message -> message.getFromUser().getId().equals(currentUserId))
                    .toList();
            mineMessages.forEach(message -> {
                messageAttachmentRepository.deleteByMessageId(message.getId());
                messageRepository.delete(message);
            });
        }
        if (yours) {
            List<Message> peerMessages = messageRepository.findByDirectChatOrder(chat.getId()).stream()
                    .filter(message -> message.getFromUser().getId().equals(peerId))
                    .toList();
            peerMessages.forEach(message -> {
                messageAttachmentRepository.deleteByMessageId(message.getId());
                messageRepository.delete(message);
            });
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSummaryDto> searchMyChats(Long currentUserId, String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        return getChats(currentUserId).stream()
                .filter(chat -> normalizedQuery.isBlank()
                        || chat.getPeerName().toLowerCase().contains(normalizedQuery)
                        || String.valueOf(chat.getPeerId()).contains(normalizedQuery)
                        || String.valueOf(chat.getId()).contains(normalizedQuery)
                        || (chat.getLastMessage() != null && chat.getLastMessage().toLowerCase().contains(normalizedQuery)))
                .toList();
    }

    public DirectChat getOrCreateChat(Long currentUserId, Long peerId) {
        if (currentUserId.equals(peerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create chat with yourself");
        }
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        User peer = userRepository.findById(peerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peer user not found"));
        if (!accountPrivacyService.canMessage(peerId, currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user cannot message this user");
        }

        return directChatRepository.findBetweenUsers(currentUserId, peerId)
                .orElseGet(() -> directChatRepository.save(new DirectChat(null, currentUser, peer, null)));
    }

    private ConversationTarget resolveTarget(Long currentUserId, Long chatId, Long groupId) {
        if ((chatId == null && groupId == null) || (chatId != null && groupId != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pass exactly one of chat_id or group_id");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        if (chatId != null) {
            DirectChat chat = directChatRepository.findById(chatId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Chat not found"));
            ensureParticipant(chat, currentUserId);
            Long peerId = chat.getUser1().getId().equals(currentUserId) ? chat.getUser2().getId() : chat.getUser1().getId();
            return new ConversationTarget(currentUser, chat, null, peerId);
        }

        GroupChat group = groupChatRepository.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not a group member"));
        return new ConversationTarget(currentUser, null, group, group.getId());
    }

    private void validateMessageBelongs(Message message, ConversationTarget target) {
        if (target.directChat() != null) {
            if (message.getDirectChat() == null || !message.getDirectChat().getId().equals(target.directChat().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this chat");
            }
            return;
        }
        if (message.getGroup() == null || !message.getGroup().getId().equals(target.groupChat().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message does not belong to this group");
        }
    }

    private void ensureParticipant(DirectChat chat, Long currentUserId) {
        if (!chat.getUser1().getId().equals(currentUserId) && !chat.getUser2().getId().equals(currentUserId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Current user is not part of this chat");
        }
    }

    private void saveAttachments(Message message, List<Attachment> attachments) {
        for (int index = 0; index < attachments.size(); index++) {
            messageAttachmentRepository.save(new MessageAttachment(null, message, attachments.get(index), index));
        }
    }

    private MessageDto toDto(Message message, Long peerId) {
        List<AttachmentDto> attachments = messageAttachmentRepository.findByMessageIdOrderByPositionAsc(message.getId()).stream()
                .map(messageAttachment -> attachmentService.toDto(messageAttachment.getAttachment()))
                .toList();
        return new MessageDto(
                message.getId(),
                message.getUpdatedAt(),
                peerId,
                message.getFromUser().getId(),
                message.getMessageText(),
                attachments
        );
    }

    private String normalize(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String fullName(User user) {
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }

    private ChatSummaryDto toChatSummary(DirectChat chat, Long currentUserId) {
        Long peerId = chat.getUser1().getId().equals(currentUserId) ? chat.getUser2().getId() : chat.getUser1().getId();
        String peerName = chat.getUser1().getId().equals(currentUserId)
                ? fullName(chat.getUser2())
                : fullName(chat.getUser1());
        String lastMessage = messageRepository.findByDirectChatOrder(chat.getId()).stream()
                .findFirst()
                .map(Message::getMessageText)
                .orElse(null);
        return new ChatSummaryDto(chat.getId(), peerId, peerName, lastMessage);
    }

    private record ConversationTarget(User currentUser, DirectChat directChat, GroupChat groupChat, Long peerId) {
    }
}
