package com.metrica.porramundial.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.metrica.porramundial.domain.ChatMessage;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findTop50ByOrderByTimestampDesc();
}