package com.ammapickles.backend.repository;

import com.ammapickles.backend.entity.PasswordResetToken;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Find token by its value (used during reset)
    Optional<PasswordResetToken> findByToken(String token);

    // Delete old token if user requests again
    @Modifying          
    @Transactional      
    void deleteByUserId(Long userId);
}