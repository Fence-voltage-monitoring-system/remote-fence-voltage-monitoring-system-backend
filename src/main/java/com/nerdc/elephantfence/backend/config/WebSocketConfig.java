package com.nerdc.elephantfence.backend.config;

import com.nerdc.elephantfence.backend.alerts.service.AlertWebSocketHandler;
import com.nerdc.elephantfence.backend.notifications.service.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AlertWebSocketHandler alertWebSocketHandler;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final LiveHandshakeInterceptor handshake;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(alertWebSocketHandler, "/api/alerts/ws")
                .addInterceptors(handshake).setAllowedOriginPatterns("*");
        registry.addHandler(notificationWebSocketHandler, "/api/notifications/ws")
                .addInterceptors(handshake).setAllowedOriginPatterns("*");
    }
}


