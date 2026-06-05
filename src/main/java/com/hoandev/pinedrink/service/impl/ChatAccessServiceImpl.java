package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.ChatRoom;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.service.ChatAccessService;
import org.springframework.stereotype.Service;

@Service
public class ChatAccessServiceImpl implements ChatAccessService {

    @Override
    public void assertCanAccessRoom(ChatRoom room, String accountId) {
        if (room == null || accountId == null) {
            throw new BaseException(ErrorCode.CHAT_002);
        }

        boolean isCustomer = room.getCustomerAccount() != null
                && accountId.equals(room.getCustomerAccount().getId());
        boolean isAssignedStaff = room.getAssignedStaffAccount() != null
                && accountId.equals(room.getAssignedStaffAccount().getId());

        if (!isCustomer && !isAssignedStaff) {
            throw new BaseException(ErrorCode.CHAT_002);
        }
    }
}
