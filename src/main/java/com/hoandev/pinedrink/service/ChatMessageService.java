package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Chat.SendChatMessageRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import org.springframework.data.domain.Pageable;

public interface ChatMessageService {
    /**
     * Sends a chat message.
     *
     * @param request       the request containing message details
     * @param senderAccountId the ID of the sender
     * @return the response containing the sent message
     */
    ChatMessageResponse send(SendChatMessageRequest request, String senderAccountId);
    /**
     * Retrieves messages from a chat room.
     *
     * @param roomId    the ID of the chat room
     * @param accountId the ID of the account retrieving messages
     * @param pageable  the pagination information
     * @return a page of chat message responses
     */
    PageResponse<ChatMessageResponse> getMessages(String roomId, String accountId, Pageable pageable);
}
