package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);
}
