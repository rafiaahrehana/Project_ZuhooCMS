package com.businessos.config;

import com.businessos.auth.user.User;
import com.businessos.auth.user.UserRepository;
import com.businessos.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * Authenticates the WebSocket handshake. /ws is permitAll at the HTTP security layer
 * (see SecurityConfig) because a browser's native WebSocket transport cannot send a
 * custom Authorization header - the JWT has to travel as a query parameter instead
 * (?token=...), which JwtAuthFilter's header-only check would never see. This
 * interceptor does the equivalent check at the handshake itself and refuses the
 * upgrade outright for a missing/invalid/expired token, so an unauthenticated
 * socket is never established in the first place.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = extractToken(request);
        if (token == null || !jwtService.isTokenValid(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            String email = jwtService.extractEmail(token);
            User user = email != null ? userRepository.findByEmail(email).orElse(null) : null;
            if (user == null || !user.isEnabled()) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put("principal", new StompPrincipal(String.valueOf(user.getId())));
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake auth failed: {}", e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractToken(ServerHttpRequest request) {
        MultiValueMap<String, String> params =
            UriComponentsBuilder.fromUri(request.getURI()).build().getQueryParams();
        List<String> values = params.get("token");
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }
}
