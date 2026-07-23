package com.hoandev.pinedrink.configuration;

import com.hoandev.pinedrink.security.websocket.JwtStompChannelInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
/**
 * WebSocket configuration for setting up STOMP message handling.
 * Nó mở cổng /ws cho client connect WebSocket
 * Nó cấu hình STOMP broker dùng RabbitMQ
 * Nó gắn JWT interceptor để auth khi client CONNECT/SUBSCRIBE
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtStompChannelInterceptor jwtStompChannelInterceptor;
    private final RabbitMqProperties rabbitMqProperties;

    public WebSocketConfig(JwtStompChannelInterceptor jwtStompChannelInterceptor, RabbitMqProperties rabbitMqProperties) {
        this.jwtStompChannelInterceptor = jwtStompChannelInterceptor;
        this.rabbitMqProperties = rabbitMqProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        RabbitMqProperties.Stomp stomp = rabbitMqProperties.stomp();

        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(stomp.host())
                .setRelayPort(stomp.port())
                .setVirtualHost(stomp.virtualHost())
                .setClientLogin(stomp.username())
                .setClientPasscode(stomp.password())
                .setSystemLogin(stomp.username())
                .setSystemPasscode(stomp.password())
                .setSystemHeartbeatSendInterval(stomp.heartbeatSendInterval())
                .setSystemHeartbeatReceiveInterval(stomp.heartbeatReceiveInterval());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtStompChannelInterceptor);
    }
}
