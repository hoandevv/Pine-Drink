package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {
    /**
     * Finds chat messages by room ID and status, ordered by creation date in descending order.
     *
     * @param roomId   The ID of the chat room
     * @param status   The status of the chat messages
     * @param pageable The pagination information
     * @return A page of chat messages matching the criteria
     */
    Page<ChatMessage> findByRoomIdAndStatusOrderByCreatedAtDesc(String roomId, String status, Pageable pageable);
}
