package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Chat.CreateChatRoomRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatRoomResponse;
import org.springframework.data.domain.Pageable;

public interface ChatRoomService {
    ChatRoomResponse create(CreateChatRoomRequest request, String customerAccountId);
    ChatRoomResponse getById(String roomId, String accountId);
    PageResponse<ChatRoomResponse> getMyRooms(String accountId, Pageable pageable);
}
