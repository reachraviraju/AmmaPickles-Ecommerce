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
    private final com.ammapickles.backend.repository.AddressRepository addressRepository;
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
            List<Address> addresses = addressRepository.findByUserId(userId);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String fullAddr = addr.getName() + ", " + addr.getStreet() + ", " + addr.getCity() + ", " + addr.getState() + " - " + addr.getPincode();
                builder.deliveryAddress(fullAddr);
                if (phone.isBlank() || phone.length() < 10) {
                    builder.phoneNumber(addr.getMobileNumber());
                }
                if (customerName.isBlank() || "Customer".equalsIgnoreCase(customerName)) {
                    builder.customerName(addr.getName());
                }
            }
        }

        customOrderRepo.save(builder.build());
        log.info("Successfully created custom pickle order via Gemini for session {}", sessionId);
    }

    private static final List<String> INGREDIENT_OPTIONS = List.of(
            "Mango (Avakaya)", "Lemon (Nimmakaya)", "Ginger (Allam)",
            "Tomato (Tomato)", "Mixed Vegetable", "Red Chilli (Mirapakaya)",
            "Amla (Usirikaya)", "Garlic (Velluli)", "Gongura"
    );

    private static final List<String> OIL_OPTIONS = List.of(
            "Sesame Oil (Nuvvula Nune)", "Mustard Oil (Aavala Nune)",
            "Groundnut Oil (Verusenaga Nune)", "Coconut Oil",
            "No Preference (Chef's Choice)"
    );

    private static final List<String> SPICE_OPTIONS = List.of(
            "Mild (తక్కువ కారం)", "Medium (మధ్యస్తం)", "Hot (ఎక్కువ కారం)", "Extra Hot (చాలా కారం)"
    );

    private static final List<String> SALT_OPTIONS = List.of(
            "Low Salt", "Medium Salt", "High Salt"
    );

    private static final List<String> EXTRA_OPTIONS = List.of(
            "None", "Extra Garlic", "Fenugreek (Menthi)", "Curry Leaves", "Hing (Asafoetida)"
    );

    private static final List<String> SPECIAL_OPTIONS = List.of(
            "None", "Less Oil", "Extra Tangy", "Glass Jar Packaging"
    );

    private static final List<String> QUANTITY_OPTIONS = List.of(
            "2kg", "3kg", "5kg", "10kg"
    );

    private static final Set<String> GREETINGS = Set.of(
            "hi", "hello", "hey", "namaste", "namaskaram", "hai", "hola",
            "good morning", "good afternoon", "good evening", "vanakkam", "hi there", "hello there"
    );

    private static final Set<String> FILLERS = Set.of(
            "ok", "okay", "yes", "no", "bye", "test", "thanks", "thank you", "k", "sure",
            "idk", "nothing", "whatever", "random", "anything", "testing", "demo", "sample",
            "asdf", "qwerty", "blah", "none", "na", "n/a", "nil", "food", "pickle"
    );

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
                             "**What is the main ingredient for your pickle?**\n" +
                             "_(Select a popular option below, or type any custom vegetable, fruit, or non-veg ingredient!)_";
                options = INGREDIENT_OPTIONS;
            }
            case 1 -> {
                String faq = checkGeneralInquiry(userMessage);
                if (faq != null) {
                    botMessage = faq + "\n\n**What is the main ingredient for your pickle?**\n" +
                                 "_(Select from below or type any custom ingredient!)_";
                    options = INGREDIENT_OPTIONS;
                } else if (isPureGreeting(userMessage)) {
                    botMessage = "👋 **Hello! Welcome to Amma Pickles!**\n\n" +
                                 "To begin crafting your custom batch, **what is the main ingredient for your pickle?**\n" +
                                 "_(Choose below or type your custom vegetable, fruit, or meat preference)_";
                    options = INGREDIENT_OPTIONS;
                } else if (isFillerOrInvalid(userMessage)) {
                    botMessage = "⚠️ Please select one of the popular ingredients below or type the vegetable, fruit, or meat you'd like us to pickle (e.g. Mango, Gongura, Cauliflower, Chicken):";
                    options = INGREDIENT_OPTIONS;
                } else {
                    botMessage = "Excellent choice! 👌 **" + userMessage.trim() + "** will make a wonderful pickle.\n\n" +
                                 "**Which oil do you prefer for your pickle?**\n" +
                                 "_(Oil gives the pickle its traditional flavor and longevity)_";
                    options = OIL_OPTIONS;
                }
            }
            case 2 -> {
                String lower = userMessage != null ? userMessage.toLowerCase() : "";
                if (lower.contains("which") || lower.contains("best") || lower.contains("recommend") || lower.contains("suggest")) {
                    botMessage = "💡 **Amma's Recommendation:**\n\n" +
                                 "For traditional Andhra pickles, **Cold-Pressed Sesame Oil (Nuvvula Nune)** or **Groundnut Oil** gives the most authentic aroma and rich flavor! Mustard oil is great for northern tangy pungency.\n\n" +
                                 "**Which oil would you prefer?**";
                    options = OIL_OPTIONS;
                } else if (isPureGreeting(userMessage)) {
                    botMessage = "👋 Please choose your preferred oil for the pickle from the options below, or type your choice:";
                    options = OIL_OPTIONS;
                } else if (isFillerOrInvalid(userMessage)) {
                    botMessage = "⚠️ Please choose an oil from the options below or specify your preference (e.g. Sesame Oil, Groundnut Oil, or Chef's Choice):";
                    options = OIL_OPTIONS;
                } else {
                    botMessage = "🌶️ **What spice level do you prefer?**";
                    options = SPICE_OPTIONS;
                }
            }
            case 3 -> {
                if (isPureGreeting(userMessage) || isFillerOrInvalid(userMessage) || !isValidSpice(userMessage)) {
                    botMessage = "⚠️ Please select your preferred spice level from the options below:";
                    options = SPICE_OPTIONS;
                } else {
                    botMessage = "🧂 **What salt level would you like?**";
                    options = SALT_OPTIONS;
                }
            }
            case 4 -> {
                if (isPureGreeting(userMessage) || isFillerOrInvalid(userMessage) || !isValidSalt(userMessage)) {
                    botMessage = "⚠️ Please select your preferred salt level from the options below:";
                    options = SALT_OPTIONS;
                } else {
                    botMessage = "🧄 **Any additional ingredients you'd like?**\n\n" +
                                 "Examples: Extra garlic, fenugreek seeds (menthi), curry leaves, hing (asafoetida)\n\n" +
                                 "Type your preferences or choose **\"None\"**:";
                    options = EXTRA_OPTIONS;
                }
            }
            case 5 -> {
                if (isPureGreeting(userMessage)) {
                    botMessage = "👋 Would you like any extra ingredients in your batch? Choose below or say **\"None\"**:";
                    options = EXTRA_OPTIONS;
                } else {
                    botMessage = "📝 **Any other special requests?**\n\n" +
                                 "Examples: Less oil, extra tangy, organic ingredients only, specific packaging\n\n" +
                                 "Type your request or choose **\"None\"**:";
                    options = SPECIAL_OPTIONS;
                }
            }
            case 6 -> {
                if (isPureGreeting(userMessage)) {
                    botMessage = "👋 Any other special requests? Choose below or say **\"None\"**:";
                    options = SPECIAL_OPTIONS;
                } else {
                    botMessage = "📦 **How much quantity do you need?**\n" +
                                 "_(Minimum order: 2kg for custom batches)_";
                    options = QUANTITY_OPTIONS;
                }
            }
            case 7 -> {
                if (isQuantityUnderMin(userMessage)) {
                    botMessage = "⚠️ **Minimum order is 2kg** for custom handmade batches to ensure authentic traditional preparation and proper fermentation.\n\n" +
                                 "Please choose at least 2kg:";
                    options = QUANTITY_OPTIONS;
                } else if (!isValidQuantity(userMessage)) {
                    botMessage = "⚠️ Please specify a quantity of at least 2kg (e.g. 2kg, 3kg, 5kg):";
                    options = QUANTITY_OPTIONS;
                } else {
                    botMessage = "Almost done! 👤\n\n**What is your name?**";
                }
            }
            case 8 -> {
                String name = userMessage != null ? userMessage.trim() : "";
                if (isPureGreeting(name)) {
                    botMessage = "👋 Please tell us your name so our team knows who this custom batch is for:";
                } else if (name.length() < 2 || name.matches("^[0-9\\W_]+$")) {
                    botMessage = "⚠️ Please enter a valid name (at least 2 letters).";
                } else {
                    botMessage = "📱 **Please provide your 10-digit mobile number**\n" +
                                 "so our kitchen team can call you to discuss pricing and delivery.";
                }
            }
            case 9 -> {
                String phone = userMessage != null ? userMessage.trim().replaceAll("[^0-9+]", "") : "";
                String digits = phone.replaceAll("[^0-9]", "");
                if (digits.length() != 10 && digits.length() != 12) {
                    botMessage = "⚠️ Please enter a valid 10-digit mobile number.\nExample: 9876543210";
                } else {
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

    private String checkGeneralInquiry(String msg) {
        if (msg == null) return null;
        String lower = msg.toLowerCase();
        if (lower.contains("price") || lower.contains("cost") || lower.contains("rate") || lower.contains("how much") || lower.contains("charges")) {
            return "💡 **Custom Batch Pricing:**\n\n" +
                   "Custom batch pricing typically ranges between **₹400–₹800/kg** for vegetarian varieties (Mango, Gongura, Garlic, Lemon, etc.) and **₹900–₹1400/kg** for non-veg varieties (Chicken, Mutton, Prawns).\n\n" +
                   "Our kitchen team will confirm exact pricing during your call before preparation!";
        }
        if (lower.contains("delivery") || lower.contains("shipping") || lower.contains("courier") || lower.contains("dispatch")) {
            return "🚚 **Delivery Information:**\n\n" +
                   "We safely pack and ship orders across India via express courier (and provide double-sealed leakproof packaging for international travel)!";
        }
        if (lower.contains("why 2kg") || lower.contains("minimum 2") || lower.contains("less than 2") || lower.contains("only 1kg") || lower.contains("only 500")) {
            return "📌 **Why Minimum 2kg?**\n\n" +
                   "Artisanal Andhra pickles require specific batch quantities for proper oil heating, stone-ground spice blending, and authentic fermentation. Batches below 2kg cannot develop true traditional aroma!";
        }
        return null;
    }

    private boolean isPureGreeting(String msg) {
        if (msg == null) return false;
        String clean = msg.trim().toLowerCase().replaceAll("[!.,?~\\-]+", " ").trim();
        return GREETINGS.contains(clean);
    }

    private boolean isFillerOrInvalid(String msg) {
        if (msg == null) return true;
        String clean = msg.trim().toLowerCase().replaceAll("[!.,?~\\-]+", " ").trim();
        if (clean.length() < 2 || FILLERS.contains(clean)) return true;

        // Pure digits or symbols e.g. "12345", "!@#$"
        if (clean.matches("^[0-9\\W_]+$")) return true;

        // Repeated characters e.g. "aaaaa", "zzzz"
        if (clean.matches("^(.)\\1{2,}$")) return true;

        // Keyboard smash with no vowels (e.g. "asdfghjk", "sdfgh")
        if (clean.length() >= 4 && !clean.matches(".*[aeiouy\\d].*")) return true;

        return false;
    }

    private boolean isValidSpice(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("mild") || lower.contains("medium") || lower.contains("hot") ||
               lower.contains("spicy") || lower.contains("కారం") || lower.contains("extra");
    }

    private boolean isValidSalt(String msg) {
        if (msg == null) return false;
        String lower = msg.toLowerCase();
        return lower.contains("salt") || lower.contains("low") || lower.contains("medium") ||
               lower.contains("high") || lower.contains("normal") || lower.contains("ఉప్పు");
    }

    private boolean isQuantityUnderMin(String msg) {
        if (msg == null) return false;
        String clean = msg.toLowerCase().replaceAll("\\s+", "");
        if (clean.matches("^(?:500g|250g|100g|1kg|1\\.5kg|0\\.5kg|1)$")) {
            return true;
        }
        return clean.contains("500g") || clean.contains("250g") || clean.contains("1kg") || clean.contains("1kilo");
    }

    private boolean isValidQuantity(String msg) {
        if (msg == null || isPureGreeting(msg) || isFillerOrInvalid(msg)) return false;
        if (isQuantityUnderMin(msg)) return false;
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(msg);
        if (m.find()) {
            try {
                double val = Double.parseDouble(m.group(1));
                return val >= 2.0;
            } catch (NumberFormatException ignored) {}
        }
        String lower = msg.toLowerCase();
        return lower.contains("2") || lower.contains("3") || lower.contains("4") ||
               lower.contains("5") || lower.contains("10") || lower.contains("two") || lower.contains("three");
    }

    private boolean isNonAdvancing(String botMessage) {
        return botMessage != null && (
            botMessage.startsWith("⚠️") || 
            botMessage.startsWith("👋") || 
            botMessage.startsWith("ℹ️") ||
            botMessage.startsWith("💡") ||
            botMessage.startsWith("🚚") ||
            botMessage.startsWith("📌")
        );
    }

    private int determineRuleBasedStep(List<ChatMessage> history) {
        int botCount = 0;
        for (ChatMessage msg : history) {
            if ("BOT".equals(msg.getSender()) && !isNonAdvancing(msg.getMessage())) {
                botCount++;
            }
        }
        return botCount;
    }

    private CustomOrderRequest buildOrderFromHistory(String sessionId, List<ChatMessage> history,
                                                      String phone, Long userId) {
        List<String> validResponses = new ArrayList<>();
        for (ChatMessage msg : history) {
            if ("BOT".equals(msg.getSender()) && isNonAdvancing(msg.getMessage())) {
                if (!validResponses.isEmpty()) {
                    validResponses.remove(validResponses.size() - 1);
                }
            } else if ("USER".equals(msg.getSender())) {
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
            List<Address> addresses = addressRepository.findByUserId(userId);
            if (addresses != null && !addresses.isEmpty()) {
                Address addr = addresses.get(0);
                String fullAddr = addr.getName() + ", " + addr.getStreet() + ", " + addr.getCity() + ", " + addr.getState() + " - " + addr.getPincode();
                builder.deliveryAddress(fullAddr);
                if (phone.isBlank() || phone.length() < 10) {
                    builder.phoneNumber(addr.getMobileNumber());
                }
                if (customerName.isBlank() || "Customer".equalsIgnoreCase(customerName)) {
                    builder.customerName(addr.getName());
                }
            }
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

    /**
     * Cleanup old chat session messages to prevent unbounded table growth.
     * Can be invoked via scheduled job or admin maintenance endpoint.
     */
    @org.springframework.transaction.annotation.Transactional
    public int cleanupOldChatMessages(int daysOld) {
        java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(daysOld);
        int deleted = chatMessageRepo.deleteByTimestampBefore(cutoff);
        log.info("Cleaned up {} chat messages older than {} days (cutoff: {})", deleted, daysOld, cutoff);
        return deleted;
    }
}
