package com.ammapickles.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Secure UUID token sent in the email link
    @Column(nullable = false, unique = true)
    private String token;

    // Which user this token belongs to
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Token expires after 1 hour
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    // Check if token is still valid
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}