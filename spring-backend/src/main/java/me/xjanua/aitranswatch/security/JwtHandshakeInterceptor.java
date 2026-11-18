package me.xjanua.aitranswatch.security;

import java.util.Map;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import io.jsonwebtoken.JwtException;
import me.xjanua.aitranswatch.enums.UserStatus;
import me.xjanua.aitranswatch.model.User;
import me.xjanua.aitranswatch.repository.UserRepository;
import me.xjanua.aitranswatch.service.JwtService;

/**
 * Xác thực JWT cho kết nối WebSocket/STOMP.
 *
 * <p>Trình duyệt không đặt được header tùy biến khi mở WebSocket, nên token được truyền qua
 * tham số truy vấn {@code ?token=...}. Token phải hợp lệ và tài khoản phải đang ACTIVE;
 * nếu không, yêu cầu nâng cấp giao thức bị từ chối ngay ở bước handshake.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtHandshakeInterceptor(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            String username = jwtService.extractUsername(token);
            User user = userRepository.findByUsernameIgnoreCase(username)
                    .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                    .orElse(null);
            if (user == null) {
                return false;
            }
            attributes.put("username", user.getUsername());
            attributes.put("userId", user.getId().toString());
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
            WebSocketHandler wsHandler, Exception exception) {
        // Không cần xử lý sau handshake.
    }
}
