package com.hoandev.pinedrink.service;

import com.hoandev.pinedrink.entity.ChatRoom;

public interface ChatAccessService {
    void assertCanAccessRoom(ChatRoom room, String accountId);
}
