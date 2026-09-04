package com.ammapickles.backend.service;

import com.ammapickles.backend.entity.*;
import com.ammapickles.backend.repository.ChatMessageRepository;
import com.ammapickles.backend.repository.CustomOrderRequestRepository;
import com.ammapickles.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI-powered Custom Pickle Chat Service backed by Google Gemini.
 * Features:
 * - Conversational AI agent ("Amma's Master Pickle Chef")
 * - Collects: Main Ingredient, Oil, Spice, Salt, Extra Ingredients, Special Requests, Quantity (min 2kg), Name, Phone
 * - Automatically parses final order JSON and creates CustomOrderRequest entity
 * - Robust fallback to rule-based conversation if Gemini API is unreachable or not configured
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomPickleChatService {

    private final ChatMessageRepository chatMessageRepo;
    private final CustomOrderRequestRepository customOrderRepo;
    private final UserRepository userRepository;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Pattern CUSTOM_ORDER_BLOCK_PATTERN = Pattern.compile(
            "```(?:custom_order)?\\s*(\\{[\\s\\S]*?\\})\\s*```",
            Pattern.CASE_INSENSITIVE
    );

    private static final List<String> POPULAR_SUGGESTIONS = List.of(
            "Mango Avakaya (2kg)", "Lemon Pickle (2kg)", "Gongura Pickle (2kg)",
            "Ginger (Allam) (3kg)", "Tomato Pickle (2kg)", "Red Chilli Pickle (2kg)"
    );

    /**
     * Process an incoming message or start the conversation.
     */
    @Transactional
    public Map<String, Object> processMessage(String sessionId, String userMessage, Long userId) {
        Map<String, Object> response = new LinkedHashMap<>();

        // Save incoming user message if present
        if (userMessage != null && !userMessage.isBlank()) {
            ChatMessage userMsg = ChatMessage.builder()
                    .sessionId(sessionId)
                    .sender("USER")
                    .message(userMessage.trim())
                    .build();
            chatMessageRepo.save(userMsg);
        }

        // Fetch session conversation history
        List<ChatMessage> history = chatMessageRepo.findBySessionIdOrderByTimestampAsc(sessionId);

        // Try processing with Gemini AI first
        if (geminiService.isConfigured()) {
            try {
                return processWithGemini(sessionId, history, userId);
            } catch (Exception e) {
                log.warn("Gemini API call failed, falling back to rule-based flow. Reason: {}", e.getMessage());
            }
        } else {
            log.info("Gemini API key not configured. Using rule-based conversation flow.");
        }

        // Fallback to robust rule-based flow
        return processWithRuleBasedFallback(sessionId, userMessage, history, userId);
    }

    /**
     * Process conversation using Gemini AI.
     */
    private Map<String, Object> processWithGemini(String sessionId, List<ChatMessage> history, Long userId) throws Exception {
        List<Map<String, String>> formattedHistory = new ArrayList<>();
        for (ChatMessage msg : history) {
            formattedHistory.add(Map.of(
                    "sender", msg.getSender(),
                    "message", msg.getMessage()
            ));
        }

        String geminiRawResponse = geminiService.generateChatResponse(formattedHistory);
        log.debug("Gemini raw response for session {}: {}", sessionId, geminiRawResponse);

        // Check if Gemini completed the order and outputted the custom_order block
        Matcher matcher = CUSTOM_ORDER_BLOCK_PATTERN.matcher(geminiRawResponse);
        boolean completed = false;
        String cleanMessage = geminiRawResponse;

        if (matcher.find()) {
            String jsonPayload = matcher.group(1);
            try {
                JsonNode orderJson = objectMapper.readTree(jsonPayload);
                if (orderJson.path("orderComplete").asBoolean(false)) {
                    saveOrderFromAi(sessionId, orderJson, userId);
                    completed = true;
                    // Remove the raw code block from the message shown to the user
                    cleanMessage = geminiRawResponse.replaceAll("```(?:custom_order)?[\\s\\S]*?```", "").trim();
                }
            } catch (Exception ex) {
                log.error("Failed to parse custom_order JSON from Gemini response: {}", jsonPayload, ex);
            }
        }

        // Save bot message in chat history
        saveBotMessage(sessionId, cleanMessage);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", cleanMessage);
        response.put("completed", completed);
        if (history.isEmpty() || history.size() <= 1) {
            response.put("options", POPULAR_SUGGESTIONS);
        }
        return response;
    }

    /**
     * Save order entity from AI-generated JSON.
     */
    private void saveOrderFromAi(String sessionId, JsonNode json, Long userId) {
        String pickleType = json.path("pickleType").asText("Not specified");
        String oil = json.path("oilPreference").asText("Chef's Choice");
        String spice = json.path("spiceLevel").asText("Medium");
        String salt = json.path("saltLevel").asText("Medium");
        String additional = json.path("additionalIngredients").asText(null);
        String special = json.path("specialInstructions").asText(null);
        String quantity = json.path("quantity").asText("2kg");
        String customerName = json.path("customerName").asText("Customer");
        String phone = json.path("phoneNumber").asText("");

        // Normalize phone
        phone = phone.replaceAll("[^0-9+]", "");

        CustomOrderRequest.CustomOrderRequestBuilder builder = CustomOrderRequest.builder()
                .sessionId(sessionId)
                .customerName(customerName)
                .phoneNumber(phone)
                .pickleType(pickleType)
                .oilPreference(oil)
                .spiceLevel(spice)
                .saltLevel(salt)
                .additionalIngredients("None".equalsIgnoreCase(additional) ? null : additional)
                .specialInstructions("None".equalsIgnoreCase(special) ? null : special)
                .quantity(quantity)
                .status(CustomOrderStatus.NEW);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(builder::user);
        }

        customOrderRepo.save(builder.build());
        log.info("Successfully created custom pickle order via Gemini for session {}", sessionId);
    }

    /**
     * Rule-based fallback when Gemini API key is missing or network fails.
     */
    private Map<String, Object> processWithRuleBasedFallback(String sessionId, String userMessage,
                                                             List<ChatMessage> history, Long userId) {
        int step = determineRuleBasedStep(history);
        Map<String, Object> response = new LinkedHashMap<>();
        String botMessage;
        List<String> options = null;
        boolean completed = false;

        switch (step) {
            case 0 -> {
                botMessage = "🙏 **Welcome to Amma Pickles Custom Orders!**\n\n" +
                             "I'll help you craft your perfect custom pickle batch, made fresh with traditional methods.\n\n" +
                             "📌 *Minimum order: 2kg*\n\n" +
                             "**What is the main ingredient for your pickle?**";
                options = List.of(
                        "Mango (Avakaya)", "Lemon (Nimmakaya)", "Ginger (Allam)",
                        "Tomato (Tomato)", "Mixed Vegetable", "Red Chilli (Mirapakaya)",
                        "Amla (Usirikaya)", "Garlic (Velluli)", "Gongura"
                );
            }
            case 1 -> {
                botMessage = "Excellent choice! 👌\n\n" +
                             "**Which oil do you prefer for your pickle?**\n" +
                             "_(Oil gives the pickle its traditional flavor and longevity)_";
                options = List.of(
                        "Sesame Oil (Nuvvula Nune)", "Mustard Oil (Aavala Nune)",
                        "Groundnut Oil (Verusenaga Nune)", "Coconut Oil",
                        "No Preference (Chef's Choice)"
                );
            }
            case 2 -> {
                botMessage = "🌶️ **What spice level do you prefer?**";
                options = List.of("Mild (తక్కువ కారం)", "Medium (మధ్యస్తం)", "Hot (ఎక్కువ కారం)", "Extra Hot (చాలా కారం)");
            }
            case 3 -> {
                botMessage = "🧂 **What salt level would you like?**";
                options = List.of("Low Salt", "Medium Salt", "High Salt");
            }
            case 4 -> {
                botMessage = "🧄 **Any additional ingredients you'd like?**\n\n" +
                             "Examples: Extra garlic, fenugreek seeds (menthi), curry leaves, hing (asafoetida)\n\n" +
                             "Type your preferences or say **\"None\"**";
            }
            case 5 -> {
                botMessage = "📝 **Any other special requests?**\n\n" +
                             "Examples: Less oil, extra tangy, organic ingredients only, specific packaging\n\n" +
                             "Type your request or say **\"None\"**";
            }
            case 6 -> {
                botMessage = "📦 **How much quantity do you need?**\n" +
                             "_(Minimum order: 2kg)_";
                options = List.of("2kg", "3kg", "5kg", "10kg");
            }
            case 7 -> {
                botMessage = "Almost done! 👤\n\n**What is your name?**";
            }
            case 8 -> {
                String name = userMessage != null ? userMessage.trim() : "";
                if (name.length() < 2) {
                    botMessage = "⚠️ Please enter a valid name (at least 2 characters).";
                    response.put("message", botMessage);
                    response.put("completed", false);
                    response.put("retry", true);
                    saveBotMessage(sessionId, botMessage);
                    return response;
                }
                botMessage = "📱 **Please provide your 10-digit mobile number**\n" +
                             "so our kitchen team can call you to discuss pricing and delivery.";
            }
            case 9 -> {
                String phone = userMessage != null ? userMessage.trim().replaceAll("[^0-9+]", "") : "";
                String digits = phone.replaceAll("[^0-9]", "");
                if (digits.length() != 10 && digits.length() != 12) {
                    botMessage = "⚠️ Please enter a valid 10-digit mobile number.\nExample: 9876543210";
                    response.put("message", botMessage);
                    response.put("completed", false);
                    response.put("retry", true);
                    saveBotMessage(sessionId, botMessage);
                    return response;
                }

                CustomOrderRequest orderRequest = buildOrderFromHistory(sessionId, history, phone, userId);
                customOrderRepo.save(orderRequest);

                botMessage = "✅ **Your custom pickle order has been submitted!**\n\n" +
                             "📋 **Order Summary:**\n" +
                             "━━━━━━━━━━━━━━━━━━━━\n" +
                             "🥒 Main Ingredient: **" + orderRequest.getPickleType() + "**\n" +
                             "🫒 Oil: **" + orderRequest.getOilPreference() + "**\n" +
                             "🌶️ Spice Level: **" + orderRequest.getSpiceLevel() + "**\n" +
                             "🧂 Salt Level: **" + orderRequest.getSaltLevel() + "**\n" +
                             "🧄 Extra Ingredients: **" + nvl(orderRequest.getAdditionalIngredients()) + "**\n" +
                             "📝 Special Requests: **" + nvl(orderRequest.getSpecialInstructions()) + "**\n" +
                             "📦 Quantity: **" + orderRequest.getQuantity() + "**\n" +
                             "━━━━━━━━━━━━━━━━━━━━\n" +
                             "👤 Name: **" + orderRequest.getCustomerName() + "**\n" +
                             "📱 Phone: **" + orderRequest.getPhoneNumber() + "**\n\n" +
                             "🔔 **Our team will call you within 24 hours** to discuss pricing and delivery.\n\n" +
                             "Thank you for choosing Amma Pickles! 🙏";
                completed = true;
            }
            default -> {
                botMessage = "🙏 Your order has already been submitted. Our team will contact you soon!";
                completed = true;
            }
        }

        saveBotMessage(sessionId, botMessage);
        response.put("message", botMessage);
        response.put("completed", completed);
        if (options != null) {
            response.put("options", options);
        }
        return response;
    }

    private int determineRuleBasedStep(List<ChatMessage> history) {
        int botCount = 0;
        for (ChatMessage msg : history) {
            if ("BOT".equals(msg.getSender()) && !msg.getMessage().startsWith("⚠️")) {
                botCount++;
            }
        }
        return botCount;
    }

    private CustomOrderRequest buildOrderFromHistory(String sessionId, List<ChatMessage> history,
                                                      String phone, Long userId) {
        List<String> validResponses = new ArrayList<>();
        boolean skipNext = false;
        for (ChatMessage msg : history) {
            if ("BOT".equals(msg.getSender()) && msg.getMessage().startsWith("⚠️")) {
                skipNext = true;
                continue;
            }
            if ("USER".equals(msg.getSender())) {
                if (skipNext) {
                    skipNext = false;
                    continue;
                }
                validResponses.add(msg.getMessage());
            }
        }

        String pickleType = validResponses.size() > 0 ? validResponses.get(0) : "Not specified";
        String oil = validResponses.size() > 1 ? validResponses.get(1) : "Chef's Choice";
        String spice = validResponses.size() > 2 ? validResponses.get(2) : "Medium";
        String salt = validResponses.size() > 3 ? validResponses.get(3) : "Medium";
        String additional = validResponses.size() > 4 ? validResponses.get(4) : "None";
        String special = validResponses.size() > 5 ? validResponses.get(5) : "None";
        String quantity = validResponses.size() > 6 ? validResponses.get(6) : "2kg";
        String customerName = validResponses.size() > 7 ? validResponses.get(7) : "Customer";

        CustomOrderRequest.CustomOrderRequestBuilder builder = CustomOrderRequest.builder()
                .sessionId(sessionId)
                .customerName(customerName)
                .phoneNumber(phone)
                .pickleType(pickleType)
                .oilPreference(oil)
                .spiceLevel(spice)
                .saltLevel(salt)
                .additionalIngredients("None".equalsIgnoreCase(additional) ? null : additional)
                .specialInstructions("None".equalsIgnoreCase(special) ? null : special)
                .quantity(quantity)
                .status(CustomOrderStatus.NEW);

        if (userId != null) {
            userRepository.findById(userId).ifPresent(builder::user);
        }

        return builder.build();
    }

    private void saveBotMessage(String sessionId, String message) {
        ChatMessage botMsg = ChatMessage.builder()
                .sessionId(sessionId)
                .sender("BOT")
                .message(message)
                .build();
        chatMessageRepo.save(botMsg);
    }

    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepo.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    private String nvl(String value) {
        return (value == null || value.isBlank()) ? "None" : value;
    }
}
