package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Chat.SendChatMessageRequest;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.security.websocket.StompPrincipal;
import com.hoandev.pinedrink.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatRealtimeController {

    private final ChatMessageService chatMessageService;

    public ChatRealtimeController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @MessageMapping("/chat.send")
    public ChatMessageResponse send(@Valid @Payload SendChatMessageRequest request, Principal principal) {
        if (!(principal instanceof StompPrincipal stompPrincipal)) {
            throw new BaseException(ErrorCode.CHAT_004);
        }
        return chatMessageService.send(request, stompPrincipal.getName());
    }
}
