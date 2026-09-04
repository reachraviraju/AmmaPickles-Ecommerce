package com.ammapickles.backend.controller;

import com.ammapickles.backend.security.CustomUserDetails;
import com.ammapickles.backend.service.CustomPickleChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for the Custom Pickle Chatbot.
 * Serves the chat page and handles chat message REST API.
 * All errors are caught and returned as user-friendly messages.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class CustomPickleChatController {

    private final CustomPickleChatService chatService;

    /**
     * Render the chat page.
     */
    @GetMapping("/custom-pickle")
    public String chatPage(@AuthenticationPrincipal CustomUserDetails userDetails,
                           Model model) {
        if (userDetails != null) {
            model.addAttribute("username", userDetails.getUser().getUsername());
        }
        model.addAttribute("chatSessionId", UUID.randomUUID().toString());
        return "custom-pickle-chat";
    }

    /**
     * REST endpoint: start a new chat session (sends welcome message).
     */
    @PostMapping("/api/custom-pickle/chat/start")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> startChat(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String sessionId = request.get("sessionId");
            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body(errorResponse(
                    "Session ID is required. Please refresh the page."));
            }

            Long userId = userDetails != null ? userDetails.getUser().getId() : null;
            Map<String, Object> response = chatService.processMessage(sessionId, null, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error starting chat session", e);
            return ResponseEntity.ok(errorResponse(
                "⚠️ Something went wrong starting the chat. Please refresh the page and try again."));
        }
    }

    /**
     * REST endpoint: send a message and get bot response.
     */
    @PostMapping("/api/custom-pickle/chat/send")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        try {
            String sessionId = request.get("sessionId");
            String message = request.get("message");

            if (sessionId == null || sessionId.isBlank()) {
                return ResponseEntity.badRequest().body(errorResponse(
                    "Session ID is required. Please refresh the page."));
            }
            if (message == null || message.isBlank()) {
                return ResponseEntity.badRequest().body(errorResponse(
                    "Please type a message before sending."));
            }
            // Limit message length to prevent abuse
            if (message.length() > 500) {
                return ResponseEntity.ok(errorResponse(
                    "⚠️ Message is too long. Please keep it under 500 characters."));
            }

            Long userId = userDetails != null ? userDetails.getUser().getId() : null;
            Map<String, Object> response = chatService.processMessage(sessionId, message, userId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing chat message", e);
            return ResponseEntity.ok(errorResponse(
                "⚠️ Something went wrong. Please try sending your message again."));
        }
    }

    /**
     * Build a standardized error response that the frontend can handle.
     */
    private Map<String, Object> errorResponse(String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", message);
        response.put("error", true);
        response.put("completed", false);
        return response;
    }
}
