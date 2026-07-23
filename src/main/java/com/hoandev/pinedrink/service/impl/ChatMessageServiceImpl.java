package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.ChatMessage;
import com.hoandev.pinedrink.entity.ChatRoom;
import com.hoandev.pinedrink.entity.dto.request.Chat.SendChatMessageRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ChatMapper;
import com.hoandev.pinedrink.realtime.RealtimeEvent;
import com.hoandev.pinedrink.realtime.RealtimeEventFactory;
import com.hoandev.pinedrink.realtime.RealtimeEventPublisher;
import com.hoandev.pinedrink.realtime.RealtimeEventType;
import com.hoandev.pinedrink.realtime.RealtimePublishService;
import com.hoandev.pinedrink.realtime.payload.ChatMessagePayload;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.ChatMessageRepository;
import com.hoandev.pinedrink.repository.ChatRoomRepository;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ChatAccessService;
import com.hoandev.pinedrink.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AccountRepository accountRepository;
    private final ChatAccessService chatAccessService;
    private final ChatMapper chatMapper;
    private final AccessScopeService accessScopeService;
    private final RealtimeEventFactory eventFactory;
    private final RealtimePublishService realtimePublishService;
    private final RealtimeEventPublisher realtimeEventPublisher;

    public ChatMessageServiceImpl(ChatRoomRepository chatRoomRepository, ChatMessageRepository chatMessageRepository, AccountRepository accountRepository, ChatAccessService chatAccessService, ChatMapper chatMapper, AccessScopeService accessScopeService, RealtimeEventFactory eventFactory, RealtimePublishService realtimePublishService, RealtimeEventPublisher realtimeEventPublisher) {
        this.chatRoomRepository = chatRoomRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.accountRepository = accountRepository;
        this.chatAccessService = chatAccessService;
        this.chatMapper = chatMapper;
        this.accessScopeService = accessScopeService;
        this.eventFactory = eventFactory;
        this.realtimePublishService = realtimePublishService;
        this.realtimeEventPublisher = realtimeEventPublisher;
    }

    @Override
    @Transactional
    public ChatMessageResponse send(SendChatMessageRequest request, String senderAccountId) {
        validateMessage(request);
        ChatRoom room = chatRoomRepository.findById(request.roomId())
                .orElseThrow(() -> new BaseException(ErrorCode.CHAT_001));
        chatAccessService.assertCanAccessRoom(room, senderAccountId);
        Account sender = accountRepository.findById(senderAccountId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSenderAccount(sender);
        message.setSenderType(resolveSenderType(room, senderAccountId));
        message.setMessageType(normalizeMessageType(request.messageType()));
        message.setContent(request.content());
        message.setMetadata(request.metadata());
        message = chatMessageRepository.save(message);

        room.setLastMessageAt(LocalDateTime.now());
        room.setLastMessagePreview(preview(message));
        chatRoomRepository.save(room);

        ChatMessageResponse response = chatMapper.toMessageResponse(message);
        publishAfterCommit(room, response, senderAccountId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatMessageResponse> getMessages(String roomId, String accountId, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHAT_001));
        chatAccessService.assertCanAccessRoom(room, accountId);
        Page<ChatMessage> messages = chatMessageRepository.findByRoomIdAndStatusOrderByCreatedAtDesc(roomId, "ACTIVE", pageable);
        List<ChatMessageResponse> content = messages.getContent().stream()
                .map(chatMapper::toMessageResponse)
                .toList();
        return PageResponse.from(messages, content);
    }

    private void validateMessage(SendChatMessageRequest request) {
        boolean hasContent = request.content() != null && !request.content().isBlank();
        boolean hasMetadata = request.metadata() != null && !request.metadata().isBlank();
        if (!hasContent && !hasMetadata) {
            throw new BaseException(ErrorCode.CHAT_003);
        }
    }

    private String normalizeMessageType(String messageType) {
        if (messageType == null || messageType.isBlank()) {
            return "TEXT";
        }
        return messageType.trim().toUpperCase();
    }

    private String preview(ChatMessage message) {
        if ("TEXT".equals(message.getMessageType()) && message.getContent() != null) {
            String content = message.getContent().trim();
            return content.length() > 255 ? content.substring(0, 255) : content;
        }
        return "[" + message.getMessageType() + "]";
    }

    private void publishAfterCommit(ChatRoom room, ChatMessageResponse response, String senderAccountId) {
        ChatMessagePayload payload = new ChatMessagePayload(
                response.roomId(),
                response.id(),
                response.senderAccountId(),
                response.senderType(),
                response.senderName(),
                response.messageType(),
                response.content(),
                response.metadata(),
                Instant.now()
        );
        RealtimeEvent<ChatMessagePayload> event = eventFactory.create(
                RealtimeEventType.CHAT_MESSAGE_SENT,
                senderAccountId,
                "CHAT_ROOM",
                room.getId(),
                payload
        );
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                realtimePublishService.publishChatRoomEvent(room.getId(), event);
                if (room.getBranch() != null) {
                    realtimePublishService.publishBranchChatRoomEvent(room.getBranch().getId(), event);
                }
                realtimeEventPublisher.publish("chat.message.sent", event);
            }
        });
    }

    private String resolveSenderType(ChatRoom room, String senderAccountId) {
        if (room.getCustomerAccount() != null && senderAccountId.equals(room.getCustomerAccount().getId())) {
            return "CUSTOMER";
        }
        AccessScopeContext scope = accessScopeService.resolveScopeByAccountId(senderAccountId);
        if (scope.fullAccess()) {
            return "ADMIN";
        }
        return "STAFF";
    }
}
