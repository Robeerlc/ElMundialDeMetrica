package com.metrica.porramundial.controller;

import com.metrica.porramundial.domain.entity.ChatMessage;
import com.metrica.porramundial.dto.chat.ChatInputRequest;
import com.metrica.porramundial.repository.ChatMessageRepository;
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

    public ChatController(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/global")
    public ChatMessage broadcastMessage(ChatInputRequest request, Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "Anónimo";

        ChatMessage chatMessage = ChatMessage.builder()
                .username(username.split("@")[0])
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