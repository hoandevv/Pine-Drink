package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.Account;
import com.hoandev.pinedrink.entity.Branch;
import com.hoandev.pinedrink.entity.ChatRoom;
import com.hoandev.pinedrink.entity.Order;
import com.hoandev.pinedrink.entity.dto.request.Chat.CreateChatRoomRequest;
import com.hoandev.pinedrink.entity.dto.response.PageResponse;
import com.hoandev.pinedrink.entity.dto.response.Chat.ChatRoomResponse;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.mapper.ChatMapper;
import com.hoandev.pinedrink.realtime.RealtimeEvent;
import com.hoandev.pinedrink.realtime.RealtimeEventFactory;
import com.hoandev.pinedrink.realtime.RealtimeEventType;
import com.hoandev.pinedrink.realtime.RealtimePublishService;
import com.hoandev.pinedrink.repository.AccountRepository;
import com.hoandev.pinedrink.repository.BranchRepository;
import com.hoandev.pinedrink.repository.ChatRoomRepository;
import com.hoandev.pinedrink.repository.OrderRepository;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ChatAccessService;
import com.hoandev.pinedrink.service.ChatRoomService;
import com.hoandev.pinedrink.utils.CodeGenerator;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final OrderRepository orderRepository;
    private final ChatAccessService chatAccessService;
    private final ChatMapper chatMapper;
    private final CodeGenerator codeGenerator;
    private final AccessScopeService accessScopeService;
    private final RealtimeEventFactory eventFactory;
    private final RealtimePublishService realtimePublishService;

    @Override
    @Transactional
    public ChatRoomResponse create(CreateChatRoomRequest request, String customerAccountId) {
        Branch branch = resolveConversationBranch(request);
        return chatRoomRepository.findByCustomerAccountIdAndBranchIdAndStatus(customerAccountId, branch.getId(), "ACTIVE")
                .map(existingRoom -> chatMapper.toRoomResponse(refreshConversationMetadata(existingRoom, request)))
                .orElseGet(() -> chatMapper.toRoomResponse(createNewRoom(request, customerAccountId, branch)));
    }

    @Override
    @Transactional(readOnly = true)
    public ChatRoomResponse getById(String roomId, String accountId) {
        ChatRoom room = getRoomOrThrow(roomId);
        chatAccessService.assertCanAccessRoom(room, accountId);
        return chatMapper.toRoomResponse(room);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatRoomResponse> getMyRooms(String accountId, Pageable pageable) {
        Page<ChatRoom> rooms = chatRoomRepository
                .findByCustomerAccountIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(accountId, "ACTIVE", pageable);
        List<ChatRoomResponse> content = rooms.getContent().stream()
                .map(chatMapper::toRoomResponse)
                .toList();
        return PageResponse.from(rooms, content);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ChatRoomResponse> getBranchRooms(String branchId, Pageable pageable) {
        AccessScopeContext scope = accessScopeService.resolveCurrentScope();
        Page<ChatRoom> rooms;
        if (branchId != null && !branchId.isBlank()) {
            accessScopeService.assertCanAccessBranch(branchId);
            rooms = chatRoomRepository.findByBranchIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(branchId, "ACTIVE", pageable);
        } else if (scope.fullAccess()) {
            rooms = chatRoomRepository.findByStatusOrderByLastMessageAtDescCreatedAtDesc("ACTIVE", pageable);
        } else if (!scope.branchIds().isEmpty()) {
            String firstBranchId = scope.branchIds().iterator().next();
            rooms = chatRoomRepository.findByBranchIdAndStatusOrderByLastMessageAtDescCreatedAtDesc(firstBranchId, "ACTIVE", pageable);
        } else {
            throw new BaseException(ErrorCode.AUTH_007);
        }
        List<ChatRoomResponse> content = rooms.getContent().stream()
                .map(chatMapper::toRoomResponse)
                .toList();
        return PageResponse.from(rooms, content);
    }

    @Override
    @Transactional
    public ChatRoomResponse setPrimaryHandler(String roomId, String staffAccountId) {
        ChatRoom room = getRoomOrThrow(roomId);
        if (room.getBranch() != null) {
            accessScopeService.assertCanAccessBranch(room.getBranch().getId());
        } else {
            accessScopeService.assertSystemAccess();
        }
        Account staff = accountRepository.findById(staffAccountId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));
        room.setAssignedStaffAccount(staff);
        ChatRoom saved = chatRoomRepository.save(room);
        ChatRoomResponse response = chatMapper.toRoomResponse(saved);
        publishBranchRoomEvent(saved, response, staffAccountId, RealtimeEventType.CHAT_ROOM_ASSIGNED);
        return response;
    }

    private ChatRoom createNewRoom(CreateChatRoomRequest request, String customerAccountId, Branch branch) {
        Account customer = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new BaseException(ErrorCode.AUTH_012));

        ChatRoom room = new ChatRoom();
        room.setRoomCode(codeGenerator.generate("CH", customerAccountId));
        room.setCustomerAccount(customer);
        room.setBranch(branch);
        room.setTitle(request.title());

        if (request.orderId() != null && !request.orderId().isBlank()) {
            Order order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Order not found"));
            room.setOrder(order);
        }

        ChatRoom saved = chatRoomRepository.save(room);
        publishBranchRoomEvent(saved, chatMapper.toRoomResponse(saved), customerAccountId, RealtimeEventType.CHAT_ROOM_CREATED);
        return saved;
    }

    private Branch resolveConversationBranch(CreateChatRoomRequest request) {
        if (request.branchId() != null && !request.branchId().isBlank()) {
            return branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new BaseException(ErrorCode.BRANCH_001));
        }
        if (request.orderId() != null && !request.orderId().isBlank()) {
            Order order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Order not found"));
            return order.getBranch();
        }
        throw new BaseException(ErrorCode.COM_004, "Branch or order is required to start a chat conversation");
    }

    private ChatRoom refreshConversationMetadata(ChatRoom room, CreateChatRoomRequest request) {
        boolean changed = false;
        if ((room.getTitle() == null || room.getTitle().isBlank()) && request.title() != null && !request.title().isBlank()) {
            room.setTitle(request.title());
            changed = true;
        }
        if (room.getOrder() == null && request.orderId() != null && !request.orderId().isBlank()) {
            Order order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new BaseException(ErrorCode.COM_005, "Order not found"));
            room.setOrder(order);
            changed = true;
        }
        return changed ? chatRoomRepository.save(room) : room;
    }

    private void publishBranchRoomEvent(ChatRoom room, ChatRoomResponse response, String actorAccountId, String eventType) {
        if (room.getBranch() == null) {
            return;
        }
        RealtimeEvent<ChatRoomResponse> event = eventFactory.create(
                eventType,
                actorAccountId,
                "CHAT_ROOM",
                room.getId(),
                response
        );
        realtimePublishService.publishBranchChatRoomEvent(room.getBranch().getId(), event);
    }

    private ChatRoom getRoomOrThrow(String roomId) {
        return chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BaseException(ErrorCode.CHAT_001));
    }
}
