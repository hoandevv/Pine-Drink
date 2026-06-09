package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
    Optional<ChatRoom> findByCustomerAccountIdAndBranchIdAndStatus(String customerAccountId, String branchId, String status);

    /**
     * Finds chat rooms by customer account ID and status, ordered by last message date and creation date in descending order.
     *
     * @param accountId The ID of the customer account
     * @param status    The status of the chat rooms
     * @param pageable  The pagination information
     * @return A page of chat rooms matching the criteria
     */
    Page<ChatRoom> findByCustomerAccountIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(String accountId, String status, Pageable pageable);

    Page<ChatRoom> findByBranchIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(String branchId, String status, Pageable pageable);

    Page<ChatRoom> findByStatusOrderByLastMessageAtDescCreatedAtDesc(String status, Pageable pageable);
}
