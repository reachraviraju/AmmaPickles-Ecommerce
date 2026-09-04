package com.ammapickles.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Initializes database tables for Custom Pickle Chatbot and Custom Orders
 * if they do not already exist. This ensures compatibility with production environments
 * (like Render / Aiven MySQL) where hibernate.ddl-auto=none.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomPickleTableInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            log.info("Checking and creating Custom Pickle tables if not exist...");

            // 1. Create chat_messages table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS chat_messages (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(255) NOT NULL,
                    sender VARCHAR(50) NOT NULL,
                    message TEXT NOT NULL,
                    timestamp DATETIME NOT NULL,
                    INDEX idx_chat_session (session_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            // 2. Create custom_order_requests table
            jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS custom_order_requests (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    customer_name VARCHAR(255) NOT NULL,
                    phone_number VARCHAR(50) NOT NULL,
                    pickle_type VARCHAR(255) NOT NULL,
                    oil_preference VARCHAR(255) NOT NULL,
                    spice_level VARCHAR(255) NOT NULL,
                    salt_level VARCHAR(255) NOT NULL,
                    additional_ingredients TEXT,
                    special_instructions TEXT,
                    quantity VARCHAR(100) NOT NULL,
                    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
                    admin_notes TEXT,
                    user_id BIGINT,
                    session_id VARCHAR(255) NOT NULL,
                    created_at DATETIME NOT NULL,
                    updated_at DATETIME,
                    INDEX idx_custom_order_status (status),
                    INDEX idx_custom_order_user (user_id),
                    INDEX idx_custom_order_date (created_at)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
            """);

            log.info("Custom Pickle tables initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Custom Pickle tables: {}", e.getMessage(), e);
        }
    }
}
