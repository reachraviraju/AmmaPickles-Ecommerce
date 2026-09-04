package com.ammapickles.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
    @Index(name = "idx_chat_session", columnList = "sessionId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Groups messages in one conversation */
    @Column(nullable = false)
    private String sessionId;

    /** USER or BOT */
    @Column(nullable = false)
    private String sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }
}
