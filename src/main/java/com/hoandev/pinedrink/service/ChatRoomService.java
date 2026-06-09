package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.dto.request.Chat.CreateChatRoomRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatRoomResponse;
import org.springframework.data.domain.Pageable;

public interface ChatRoomService {
    /**
     * Creates a new chat room.
     *
     * @param request          the request containing chat room details
     * @param customerAccountId the ID of the customer creating the chat room
     * @return the created chat room response
     */
    ChatRoomResponse create(CreateChatRoomRequest request, String customerAccountId);
    /**
     * Retrieves a chat room by its ID.
     *
     * @param roomId   the ID of the chat room
     * @param accountId the ID of the account requesting the chat room
     * @return the chat room response
     */
    ChatRoomResponse getById(String roomId, String accountId);
    /**
     * Retrieves all chat rooms for a specific customer.
     *
     * @param accountId the ID of the customer
     * @param pageable  the pagination information
     * @return a page of chat room responses
     */
    PageResponse<ChatRoomResponse> getMyRooms(String accountId, Pageable pageable);
    /**
     * Retrieves all chat rooms for a specific branch.
     *
     * @param branchId the ID of the branch
     * @param pageable the pagination information
     * @return a page of chat room responses
     */
    PageResponse<ChatRoomResponse> getBranchRooms(String branchId, Pageable pageable);
    /**
     * Sets the primary handler for a chat room.
     *
     * @param roomId the ID of the chat room
     * @param staffAccountId the ID of the staff member taking ownership metadata
     * @return the updated chat room response
     */
    ChatRoomResponse setPrimaryHandler(String roomId, String staffAccountId);

    /**
     * Backward-compatible alias for taking ownership of a chat room.
     */
    default ChatRoomResponse assignToMe(String roomId, String staffAccountId) {
        return setPrimaryHandler(roomId, staffAccountId);
    }
}
