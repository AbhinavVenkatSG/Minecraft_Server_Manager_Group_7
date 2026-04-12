package com.minecraftmanager.websocket.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

public class ApiKeyHandshakeInterceptor implements HandshakeInterceptor {

    private static final String VALID_API_KEY = "minecraft_server_manager_key";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        String query = request.getURI().getQuery();
        
        if (query == null || !query.contains("apiKey=")) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        String apiKey = extractApiKey(query);
        if (apiKey == null || !apiKey.equals(VALID_API_KEY)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        return true;
    }

    private String extractApiKey(String query) {
        String[] params = query.split("&");
        for (String param : params) {
            if (param.startsWith("apiKey=")) {
                return param.substring(7);
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}