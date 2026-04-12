package com.minecraftmanager.websocket.config;

import com.minecraftmanager.websocket.handler.BinaryPacketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final BinaryPacketHandler binaryPacketHandler;

    public WebSocketConfig(BinaryPacketHandler binaryPacketHandler) {
        this.binaryPacketHandler = binaryPacketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(binaryPacketHandler, "/ws")
                .setAllowedOrigins("*")
                .addInterceptors(new ApiKeyHandshakeInterceptor());
    }
}