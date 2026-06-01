package com.metrica.porramundial.controller;

import com.metrica.porramundial.domain.entity.ChatMessage;
import com.metrica.porramundial.domain.entity.User;
import com.metrica.porramundial.dto.chat.ChatInputRequest;
import com.metrica.porramundial.repository.ChatMessageRepository;
import com.metrica.porramundial.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.util.List;

@Controller
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatController(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/global")
    public ChatMessage broadcastMessage(@Valid ChatInputRequest request, Authentication authentication) {
        String email = authentication != null ? authentication.getName() : "";
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        ChatMessage chatMessage = ChatMessage.builder()
                .username(user.getFullName().split("@")[0])
                .avatar(user.getAvatar())
                .country(user.getCountry())
                .message(request.message())
                .timestamp(LocalDateTime.now())
                .build();

        return chatMessageRepository.save(chatMessage);
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public List<ChatMessage> getChatHistory() {
        return chatMessageRepository.findTop50ByOrderByTimestampDesc();
    }
}