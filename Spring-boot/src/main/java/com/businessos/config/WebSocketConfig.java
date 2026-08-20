package com.businessos.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .addInterceptors(webSocketAuthInterceptor)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    // The raw HTTP handshake request carries no Spring Security
                    // Authentication (/ws is permitAll - see SecurityConfig) since a
                    // browser's native WebSocket transport can't send an Authorization
                    // header. WebSocketAuthInterceptor validates the ?token= query
                    // param instead and stashes the resolved principal in
                    // `attributes`; pull it back out here so it becomes this STOMP
                    // session's Principal.
                    @Override
                    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                                       Map<String, Object> attributes) {
                        Object principal = attributes.get("principal");
                        return principal instanceof Principal p ? p : super.determineUser(request, wsHandler, attributes);
                    }
                });
        // Deliberately NOT .withSockJS() - every supported browser has native
        // WebSocket, and SockJS's client library assumes a Node-style `global`
        // object that modern bundlers (esbuild/Vite, which Angular now uses) don't
        // polyfill, breaking at runtime. Plain WebSocket avoids that dependency
        // entirely; HandshakeInterceptor/DefaultHandshakeHandler above work
        // identically either way.
    }
}
