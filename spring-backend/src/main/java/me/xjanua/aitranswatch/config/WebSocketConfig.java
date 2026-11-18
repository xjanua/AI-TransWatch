package me.xjanua.aitranswatch.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import me.xjanua.aitranswatch.security.JwtHandshakeInterceptor;

/**
 * Cấu hình WebSocket + STOMP cho thông báo cảnh báo thời gian thực.
 *
 * <p>Frontend kết nối tới {@code ws://host:8080/ws?token=<JWT>} và đăng ký kênh
 * {@code /topic/alerts} để nhận cảnh báo mới ngay khi rule engine phát sinh, không phải chờ
 * chu kỳ làm mới của dashboard.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Value("${frontend.allowed-origins}")
    private String allowedOrigins;

    public WebSocketConfig(JwtHandshakeInterceptor jwtHandshakeInterceptor) {
        this.jwtHandshakeInterceptor = jwtHandshakeInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins.toArray(String[]::new))
                .addInterceptors(jwtHandshakeInterceptor);
    }
}
