package com.hoandev.pinedrink.controller;

import com.hoandev.pinedrink.entity.dto.request.Chat.CreateChatRoomRequest;
import com.hoandev.pinedrink.entity.dto.response.BaseResponse;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatMessageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatRoomResponse;
import com.hoandev.pinedrink.security.UserPrincipal;
import com.hoandev.pinedrink.service.ChatMessageService;
import com.hoandev.pinedrink.service.ChatRoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
/**
 * Controller for managing chat rooms.
 */
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    /**
     * Create a new chat room.
     *
     * @param request the request to create a chat room
     * @param user    the authenticated user
     * @return the created chat room
     */
    @PostMapping
    public ResponseEntity<BaseResponse<ChatRoomResponse>> create(
            @Valid @RequestBody CreateChatRoomRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        ChatRoomResponse response = chatRoomService.create(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(response, "Chat room created successfully"));
    }
    /**
     * Get all chat rooms for the authenticated user.
     *
     * @param user    the authenticated user
     * @param pageable the pagination information
     * @return the list of chat rooms
     */
    @GetMapping
    public ResponseEntity<BaseResponse<PageResponse<ChatRoomResponse>>> getMyRooms(
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(chatRoomService.getMyRooms(user.getId(), pageable)));
    }
    /**
     * Get all chat rooms for a specific branch.
     *
     * @param branchId the ID of the branch
     * @param pageable the pagination information
     * @return the list of chat rooms
     */
    @GetMapping("/staff")
    public ResponseEntity<BaseResponse<PageResponse<ChatRoomResponse>>> getBranchRooms(
            @RequestParam(required = false) String branchId,
            @PageableDefault(size = 20, sort = "lastMessageAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(chatRoomService.getBranchRooms(branchId, pageable)));
    }
    /**
     * Get a chat room by its ID.
     *
     * @param roomId the ID of the chat room
     * @param user   the authenticated user
     * @return the chat room
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<BaseResponse<ChatRoomResponse>> getById(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(BaseResponse.success(chatRoomService.getById(roomId, user.getId())));
    }
    /**
     * Get messages for a specific chat room.
     *
     * @param roomId the ID of the chat room
     * @param user   the authenticated user
     * @param pageable the pagination information
     * @return the list of chat messages
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<BaseResponse<PageResponse<ChatMessageResponse>>> getMessages(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal user,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(BaseResponse.success(chatMessageService.getMessages(roomId, user.getId(), pageable)));
    }
}
