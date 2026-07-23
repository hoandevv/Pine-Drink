package com.hoandev.pinedrink.security.websocket;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
public class StompSubscribeGuard {

    public void assertCanSubscribe(Principal principal, String destination) {
        if (destination == null) {
            return;
        }

        if (destination.startsWith("/user/queue/")) {
            requireAuthenticated(principal);
        }
        if (destination.startsWith("/topic/orders.")
                || destination.startsWith("/topic/branches.")
                || destination.startsWith("/topic/chat.rooms.")) {
            requireAuthenticated(principal);
        }
    }

    private void requireAuthenticated(Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Authentication is required for this realtime destination");
        }
    }
}
