package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Chat.SendChatMessageRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {
    ChatMessageResponse send(SendChatMessageRequest request, String senderAccountId);
    PageResponse<ChatMessageResponse> getMessages(String roomId, String accountId, Pageable pageable);
}
