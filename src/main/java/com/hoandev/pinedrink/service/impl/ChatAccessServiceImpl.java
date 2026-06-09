package com.hoandev.pinedrink.service.impl;

import com.hoandev.pinedrink.entity.ChatRoom;
import com.hoandev.pinedrink.exception.BaseException;
import com.hoandev.pinedrink.exception.ErrorCode;
import com.hoandev.pinedrink.security.scope.AccessScopeContext;
import com.hoandev.pinedrink.service.AccessScopeService;
import com.hoandev.pinedrink.service.ChatAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatAccessServiceImpl implements ChatAccessService {

    private final AccessScopeService accessScopeService;

    @Override
    public void assertCanAccessRoom(ChatRoom room, String accountId) {
        if (room == null || accountId == null) {
            throw new BaseException(ErrorCode.CHAT_002);
        }

        boolean isCustomer = room.getCustomerAccount() != null
                && accountId.equals(room.getCustomerAccount().getId());
        boolean hasFullAccess = false;
        boolean hasBranchAccess = false;
        if (!isCustomer) {
            AccessScopeContext scope = accessScopeService.resolveScopeByAccountId(accountId);
            hasFullAccess = scope.fullAccess();
            hasBranchAccess = room.getBranch() != null && scope.branchIds().contains(room.getBranch().getId());
        }

        if (!isCustomer && !hasFullAccess && !hasBranchAccess) {
            throw new BaseException(ErrorCode.CHAT_002);
        }
    }
}
