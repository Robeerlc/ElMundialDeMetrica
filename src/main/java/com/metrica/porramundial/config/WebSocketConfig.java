package com.metrica.porramundial.config;

import com.metrica.porramundial.config.security.JwtService;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    public WebSocketConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-chat")
                .setAllowedOrigins("https://porramundialmetrica-championsfinal-kb4lt-82bcf1-193-70-44-51.sslip.io")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    List<String> authorization = accessor.getNativeHeader("Authorization");
                    if (authorization != null && !authorization.isEmpty()) {
                        String tokenHeader = authorization.getFirst();
                        if (tokenHeader != null && tokenHeader.startsWith("Bearer ")) {
                            String token = tokenHeader.substring(7);
                            try {
                                String email = jwtService.extractUsername(token);
                                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(email, null, List.of());
                                accessor.setUser(auth);
                            } catch (Exception e) {
                                System.err.println("Token JWT inválido en WebSocket: " + e.getMessage());
                                throw new IllegalArgumentException("Token inválido o expirado");
                            }
                        } else {
                            System.err.println("Formato de token incorrecto en WebSocket");
                            throw new IllegalArgumentException("El token debe empezar por Bearer");
                        }
                    } else {
                        System.err.println("Intento de conexión al WebSocket sin cabecera Authorization");
                        throw new IllegalArgumentException("Acceso denegado: Cabecera Authorization requerida");
                    }
                }
                return message;
            }
        });
    }
}