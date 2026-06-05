package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    /**
     * Finds a chat room by its order ID and customer account ID.
     *
     * @param orderId              The ID of the order
     * @param customerAccountId The ID of the customer account
     * @return An Optional containing the chat room if found, otherwise empty
     */
    Optional<ChatRoom> findByOrderIdAndCustomerAccountId(String orderId, String customerAccountId);

    /**
     * Finds chat rooms by customer account ID and status, ordered by last message date and creation date in descending order.
     *
     * @param accountId The ID of the customer account
     * @param status    The status of the chat rooms
     * @param pageable  The pagination information
     * @return A page of chat rooms matching the criteria
     */
    Page<ChatRoom> findByCustomerAccountIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(String accountId, String status, Pageable pageable);
}
