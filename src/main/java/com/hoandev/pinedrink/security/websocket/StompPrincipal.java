package com.hoandev.pinedrink.security.websocket;

import com.hoandev.pinedrink.security.UserPrincipal;

import java.security.Principal;

public class StompPrincipal implements Principal {

    private final UserPrincipal userPrincipal;

    public StompPrincipal(UserPrincipal userPrincipal) {
        this.userPrincipal = userPrincipal;
    }

    @Override
    public String getName() {
        return userPrincipal.getId();
    }

    public UserPrincipal getUserPrincipal() {
        return userPrincipal;
    }
}
