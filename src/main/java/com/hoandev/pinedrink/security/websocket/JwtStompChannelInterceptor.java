package com.hoandev.pinedrink.security.websocket;

import com.hoandev.pinedrink.security.CustomUserDetailsService;
import com.hoandev.pinedrink.security.JwtTokenProvider;
import com.hoandev.pinedrink.security.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final StompSubscribeGuard subscribeGuard;

    public JwtStompChannelInterceptor(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService userDetailsService, StompSubscribeGuard subscribeGuard) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.subscribeGuard = subscribeGuard;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            subscribeGuard.assertCanSubscribe(accessor.getUser(), accessor.getDestination());
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new BadCredentialsException("Missing WebSocket access token");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Claims claims = jwtTokenProvider.parseToken(token);
        String accountId = claims.getSubject();
        UserPrincipal principal = userDetailsService.loadPrincipalById(accountId);
        accessor.setUser(new StompPrincipal(principal));
    }
}
