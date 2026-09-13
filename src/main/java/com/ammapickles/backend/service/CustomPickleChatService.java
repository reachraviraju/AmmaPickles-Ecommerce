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
            "Mango", "Lemon", "Gongura", "Ginger", "Tomato", "Red Chilli", "Chicken", "Garlic"
    );

    private static final Map<String, String> INGREDIENT_ALIASES = new LinkedHashMap<>();
    static {
        // Mango
        for (String k : List.of("mango", "avakaya", "avakai", "mamidi", "mamidikaya", "aam", "kairi", "raw mango")) {
            INGREDIENT_ALIASES.put(k, "Mango");
        }
        // Lemon
        for (String k : List.of("lemon", "nimmakaya", "nimma", "nimbu", "lime", "citron")) {
            INGREDIENT_ALIASES.put(k, "Lemon");
        }
        // Ginger
        for (String k : List.of("ginger", "allam", "adrak", "inji")) {
            INGREDIENT_ALIASES.put(k, "Ginger");
        }
        // Tomato
        for (String k : List.of("tomato", "tamatar", "tomata")) {
            INGREDIENT_ALIASES.put(k, "Tomato");
        }
        // Red Chilli
        for (String k : List.of("red chilli", "red chili", "chilli", "chili", "mirchi", "mirapakaya", "pandu mirapakaya", "pandu mirchi", "lal mirch")) {
            INGREDIENT_ALIASES.put(k, "Red Chilli");
        }
        // Gongura
        for (String k : List.of("gongura", "sorrel", "sorrel leaves", "pulicha keerai", "ambada")) {
            INGREDIENT_ALIASES.put(k, "Gongura");
        }
        // Amla
        for (String k : List.of("amla", "usirikaya", "usiri", "gooseberry", "nellikai", "awla")) {
            INGREDIENT_ALIASES.put(k, "Amla");
        }
        // Garlic
        for (String k : List.of("garlic", "vellulli", "velluli", "lahsun", "poondu", "lasun")) {
            INGREDIENT_ALIASES.put(k, "Garlic");
        }
        // Mixed Veg
        for (String k : List.of("mixed veg", "mixed vegetable", "mixed", "mix veg", "veg mix")) {
            INGREDIENT_ALIASES.put(k, "Mixed Vegetable");
        }
        // Cauliflower
        for (String k : List.of("cauliflower", "gobi", "gobhi")) {
            INGREDIENT_ALIASES.put(k, "Cauliflower");
        }
        // Chicken
        for (String k : List.of("chicken", "kodi", "murgh", "boneless chicken", "natu kodi", "country chicken")) {
            INGREDIENT_ALIASES.put(k, "Chicken");
        }
        // Mutton
        for (String k : List.of("mutton", "goat", "mamsam", "gosht", "lamb", "boneless mutton", "keema", "kheema")) {
            INGREDIENT_ALIASES.put(k, "Mutton");
        }
        // Prawns
        for (String k : List.of("prawn", "prawns", "shrimp", "royyalu", "jheenga")) {
            INGREDIENT_ALIASES.put(k, "Prawns");
        }
        // Fish
        for (String k : List.of("fish", "chepa", "machli", "vanjaram")) {
            INGREDIENT_ALIASES.put(k, "Fish");
        }
        // Crab
        for (String k : List.of("crab", "peetha", "kekada")) {
            INGREDIENT_ALIASES.put(k, "Crab");
        }
        // Drumstick
        for (String k : List.of("drumstick", "mulakkada", "munakkaya", "sehjan")) {
            INGREDIENT_ALIASES.put(k, "Drumstick");
        }
    }

    public static String resolveIngredient(String input) {
        if (input == null || input.isBlank()) return "Custom Batch";
        String clean = input.trim().toLowerCase()
                .replaceAll("\\b(pickle|pachadi|achar|batch|variety|style|taste)\\b", "")
                .trim();
        // 1. Exact match first
        for (Map.Entry<String, String> entry : INGREDIENT_ALIASES.entrySet()) {
            if (clean.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        // 2. Whole word boundary match
        for (Map.Entry<String, String> entry : INGREDIENT_ALIASES.entrySet()) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(entry.getKey()) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(clean).find()) {
                return entry.getValue();
            }
        }
        return input.trim().substring(0, 1).toUpperCase() + input.trim().substring(1);
    }

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
        CustomOrderRequest existing = customOrderRepo.findBySessionId(sessionId);
        if (existing != null) {
            log.info("Custom order already saved for session {}, skipping duplicate creation.", sessionId);
            return;
        }

        String rawPickleType = json.path("pickleType").asText("Not specified");
        String pickleType = resolveIngredient(rawPickleType);
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
            "Mango (Avakaya)", "Lemon (Nimmakaya)", "Gongura", "Ginger (Allam)",
            "Chicken", "Mutton", "Prawns", "Garlic", "Red Chilli", "Tomato"
    );

    private static final List<String> OIL_OPTIONS = List.of(
            "Sesame Oil (Nuvvula Nune)", "Groundnut Oil", "Mustard Oil", "Chef's Choice"
    );

    private static final List<String> SPICE_OPTIONS = List.of(
            "Mild", "Medium", "Hot", "Extra Hot (Andhra Style)"
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
            "good morning", "good afternoon", "good evening", "vanakkam", "hi there", "hello there",
            "whats up", "what's up", "sup", "wassup", "how are you", "who are you", "what sup"
    );

    private static final Set<String> FILLERS = Set.of(
            "ok", "okay", "yes", "no", "bye", "test", "thanks", "thank you", "k", "sure",
            "idk", "nothing", "whatever", "random", "anything", "testing", "demo", "sample",
            "asdf", "qwerty", "blah", "none", "na", "n/a", "nil", "food", "pickle"
    );

    // Comprehensive set of known valid pickling ingredients (English, Telugu, Hindi)
    private static final Set<String> KNOWN_INGREDIENTS = Set.of(
            "mango", "avakaya", "avakai", "mamidi", "mamidikaya", "aam", "kairi",
            "lemon", "nimmakaya", "nimma", "nimbu", "lime", "citron",
            "ginger", "allam", "adrak", "inji",
            "tomato", "tamatar", "tomata",
            "gongura", "sorrel", "pulicha keerai", "ambada",
            "red chilli", "red chili", "chilli", "chili", "mirchi", "mirapakaya", "pandu mirapakaya", "pandu mirchi", "lal mirch",
            "amla", "usirikaya", "usiri", "gooseberry", "nellikai", "awla",
            "garlic", "vellulli", "velluli", "lahsun", "poondu",
            "mixed veg", "mixed vegetable", "vegetable", "mix veg",
            "cauliflower", "gobi", "gobhi",
            "drumstick", "mulakkada", "munakkaya",
            "chicken", "kodi", "murgh", "boneless chicken", "natu kodi",
            "mutton", "goat", "mamsam", "gosht", "keema", "kheema", "boneless mutton",
            "prawn", "prawns", "shrimp", "royyalu",
            "fish", "chepa", "machli",
            "crab", "peetha",
            "bitter gourd", "kakarakaya", "karela",
            "brinjal", "vankaya", "baingan", "eggplant",
            "green chilli", "pachi mirchi",
            "onion", "ulli", "pyaz"
    );

    // Strictly prohibited items honoring authentic South Indian Hindu traditions
    private static final Set<String> PROHIBITED_INGREDIENTS = Set.of(
            "beef", "cow", "cow meat", "veal", "bull", "ox", "buffalo", "pork", "bacon", "ham", "pig", "swine"
    );

    private boolean isProhibitedIngredient(String text) {
        if (text == null || text.isBlank()) return false;
        String clean = text.trim().toLowerCase();
        for (String p : PROHIBITED_INGREDIENTS) {
            Pattern pat = Pattern.compile("\\b" + Pattern.quote(p) + "\\b", Pattern.CASE_INSENSITIVE);
            if (pat.matcher(clean).find()) return true;
        }
        return false;
    }

    /**
     * Check if the user text actually refers to an edible ingredient for pickling.
     */
    private boolean isValidIngredientInput(String text) {
        if (text == null || text.isBlank()) return false;
        if (isPureGreeting(text) || isFillerOrInvalid(text) || isProhibitedIngredient(text)) return false;

        String clean = text.trim().toLowerCase()
                .replaceAll("[!.,?~\\-_()]+", " ")
                .replaceAll("\\b(pickle|achar|pachadi|batch|taste|style|variety)\\b", "")
                .trim();
        if (clean.length() < 2) return false;

        // Check against known ingredients with exact match or word boundary
        for (String item : KNOWN_INGREDIENTS) {
            if (clean.equals(item)) return true;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(item) + "\\b", Pattern.CASE_INSENSITIVE);
            if (p.matcher(clean).find()) return true;
        }
        // If not directly in known list, check if user specified a vegetable, fruit, or meat
        if (clean.contains("vegetable") || clean.contains("fruit") || clean.contains("meat") || clean.contains("non veg")) {
            return true;
        }
        return false;
    }

    private boolean isValidOilInput(String msg) {
        if (msg == null || isPureGreeting(msg) || isFillerOrInvalid(msg)) return false;
        String lower = msg.toLowerCase();
        return lower.contains("oil") || lower.contains("sesame") || lower.contains("mustard") ||
               lower.contains("groundnut") || lower.contains("coconut") || lower.contains("chef") ||
               lower.contains("nuvvula") || lower.contains("aavala") || lower.contains("verusenaga") ||
               lower.contains("none") || lower.contains("any") || lower.contains("choice");
    }

    /**
     * Rule-based fallback when Gemini API key is missing or network fails.
     */
    private Map<String, Object> processWithRuleBasedFallback(String sessionId, String userMessage,
                                                             List<ChatMessage> history, Long userId) {
        int step = determineCurrentStep(history);
        Map<String, Object> response = new LinkedHashMap<>();
        String botMessage;
        List<String> options = null;
        boolean completed = false;

        switch (step) {
            case 0 -> {
                botMessage = "🙏 **Welcome to Amma Pickles Custom Orders!**\n\n" +
                             "Tell us what pickle you'd like, and our kitchen will craft it fresh using authentic traditional recipes.\n\n" +
                             "📌 *Minimum custom batch: 2kg*\n\n" +
                             "**Which main ingredient would you like to pickle?**";
                options = INGREDIENT_OPTIONS;
            }
            case 1 -> {
                if (isProhibitedIngredient(userMessage)) {
                    botMessage = "🙏 **At Amma Pickles, we honor authentic traditional South Indian culinary traditions.**\n\n" +
                                 "We do not prepare beef or pork pickles under any circumstances. For non-vegetarian varieties, our kitchen prepares fresh batches of **Chicken**, **Mutton**, **Prawns**, and **Fish**!\n\n" +
                                 "👉 Which ingredient would you like for your custom batch?";
                    options = INGREDIENT_OPTIONS;
                } else {
                    String inquiry = checkGeneralInquiry(userMessage);
                    if (inquiry != null) {
                        botMessage = inquiry + "\n\n👉 **To get started, which main ingredient would you like?**";
                        options = INGREDIENT_OPTIONS;
                    } else if (isPureGreeting(userMessage)) {
                        botMessage = "Hello! 👋 Welcome to Amma Pickles.\n\n**Which main ingredient would you like for your custom batch?**";
                        options = INGREDIENT_OPTIONS;
                    } else if (isFillerOrInvalid(userMessage) || !isValidIngredientInput(userMessage)) {
                        botMessage = "⚠️ Please select a valid pickling ingredient (such as **Mango**, **Lemon**, **Gongura**, **Ginger**, **Garlic**, **Chicken**, etc.) from the options below or type your choice:";
                        options = INGREDIENT_OPTIONS;
                    } else {
                        String resolved = resolveIngredient(userMessage);
                        botMessage = "Excellent choice! **" + resolved + "** will make a wonderful pickle. 👌\n\n" +
                                     "**Which oil do you prefer?**";
                        options = OIL_OPTIONS;
                    }
                }
            }
            case 2 -> {
                String lower = userMessage != null ? userMessage.toLowerCase() : "";
                if (lower.contains("which") || lower.contains("best") || lower.contains("recommend") || lower.contains("suggest")) {
                    botMessage = "💡 **Amma's Recommendation:**\n" +
                                 "**Cold-Pressed Sesame Oil (Nuvvula Nune)** or **Groundnut Oil** gives the most authentic aroma and rich flavor for traditional pickles.\n\n" +
                                 "**Which oil would you prefer?**";
                    options = OIL_OPTIONS;
                } else if (isPureGreeting(userMessage) || isFillerOrInvalid(userMessage) || !isValidOilInput(userMessage)) {
                    botMessage = "⚠️ Please select an oil preference (Sesame Oil, Groundnut Oil, Mustard Oil, or Chef's Choice):";
                    options = OIL_OPTIONS;
                } else {
                    botMessage = "🌶️ **What spice level do you prefer?**";
                    options = SPICE_OPTIONS;
                }
            }
            case 3 -> {
                if (isPureGreeting(userMessage) || isFillerOrInvalid(userMessage) || !isValidSpice(userMessage)) {
                    botMessage = "⚠️ Please select your preferred spice level:";
                    options = SPICE_OPTIONS;
                } else {
                    botMessage = "🧂 **What salt level would you like?**";
                    options = SALT_OPTIONS;
                }
            }
            case 4 -> {
                if (isPureGreeting(userMessage) || isFillerOrInvalid(userMessage) || !isValidSalt(userMessage)) {
                    botMessage = "⚠️ Please select your salt level preference:";
                    options = SALT_OPTIONS;
                } else {
                    botMessage = "🧄 **Any additional ingredients?**\n(Extra garlic, fenugreek/menthi, curry leaves, hing, or None)";
                    options = EXTRA_OPTIONS;
                }
            }
            case 5 -> {
                botMessage = "📝 **Any special instructions?**\n(Less oil, extra tangy, organic ingredients, or None)";
                options = SPECIAL_OPTIONS;
            }
            case 6 -> {
                botMessage = "📦 **How much quantity do you need?**\n*(Minimum order: 2kg for custom batches)*";
                options = QUANTITY_OPTIONS;
            }
            case 7 -> {
                if (isQuantityUnderMin(userMessage)) {
                    botMessage = "⚠️ **Minimum order is 2kg** for custom handmade batches to ensure proper fermentation and flavor.\n\nPlease choose at least 2kg:";
                    options = QUANTITY_OPTIONS;
                } else if (!isValidQuantity(userMessage)) {
                    botMessage = "⚠️ Please specify a quantity of at least 2kg (e.g. 2kg, 3kg, 5kg):";
                    options = QUANTITY_OPTIONS;
                } else {
                    botMessage = "Almost done! 👤\n\n**What is your full name?**";
                }
            }
            case 8 -> {
                String name = userMessage != null ? userMessage.trim() : "";
                if (isPureGreeting(name) || name.length() < 2 || name.matches("^[0-9\\W_]+$")) {
                    botMessage = "⚠️ Please enter a valid name (at least 2 letters):";
                } else {
                    botMessage = "📱 **Please enter your 10-digit mobile number**\nso our kitchen team can contact you to confirm the batch:";
                }
            }
            case 9 -> {
                String phone = userMessage != null ? userMessage.trim().replaceAll("[^0-9+]", "") : "";
                String digits = phone.replaceAll("[^0-9]", "");
                if (digits.length() != 10 && digits.length() != 12) {
                    botMessage = "⚠️ Please enter a valid 10-digit mobile number (e.g. 9876543210):";
                } else {
                    CustomOrderRequest existing = customOrderRepo.findBySessionId(sessionId);
                    CustomOrderRequest orderRequest = existing != null ? existing : buildOrderFromHistory(sessionId, history, phone, userId);
                    if (existing == null) {
                        customOrderRepo.save(orderRequest);
                    }

                    botMessage = "✅ **Your custom pickle order request has been received!**\n\n" +
                                 "📋 **Order Summary:**\n" +
                                 "• Main Ingredient: **" + orderRequest.getPickleType() + "**\n" +
                                 "• Oil: **" + orderRequest.getOilPreference() + "**\n" +
                                 "• Spice Level: **" + orderRequest.getSpiceLevel() + "**\n" +
                                 "• Salt Level: **" + orderRequest.getSaltLevel() + "**\n" +
                                 "• Extra Ingredients: **" + nvl(orderRequest.getAdditionalIngredients()) + "**\n" +
                                 "• Quantity: **" + orderRequest.getQuantity() + "**\n" +
                                 "• Name: **" + orderRequest.getCustomerName() + "**\n" +
                                 "• Phone: **" + orderRequest.getPhoneNumber() + "**\n\n" +
                                 "📞 Our kitchen team will call you within 24 hours to confirm pricing and dispatch details. Thank you!";
                    completed = true;
                }
            }
            default -> {
                botMessage = "🙏 Your custom order has already been submitted! Our team will contact you shortly.";
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
        String clean = msg.trim().toLowerCase().replaceAll("[!.,?~\\-_']+", " ").replaceAll("\\s+", " ").trim();
        return GREETINGS.contains(clean) || clean.startsWith("hi ") || clean.startsWith("hello ") || clean.startsWith("hey ");
    }

    private boolean isFillerOrInvalid(String msg) {
        if (msg == null) return true;
        String clean = msg.trim().toLowerCase().replaceAll("[!.,?~\\-_']+", " ").replaceAll("\\s+", " ").trim();
        if (clean.length() < 2 || FILLERS.contains(clean)) return true;

        // Pure digits or symbols e.g. "12345", "!@#$"
        if (clean.matches("^[0-9\\W_]+$")) return true;

        // Repeated characters e.g. "aaaaa", "zzzz"
        if (clean.matches("^(.)\\1{2,}$")) return true;

        return false;
    }

    private boolean isValidSpice(String msg) {
        if (msg == null || isPureGreeting(msg) || isFillerOrInvalid(msg)) return false;
        String lower = msg.toLowerCase();
        return lower.contains("mild") || lower.contains("medium") || lower.contains("hot") ||
               lower.contains("spicy") || lower.contains("extra");
    }

    private boolean isValidSalt(String msg) {
        if (msg == null || isPureGreeting(msg) || isFillerOrInvalid(msg)) return false;
        String lower = msg.toLowerCase();
        return lower.contains("salt") || lower.contains("low") || lower.contains("medium") ||
               lower.contains("high") || lower.contains("normal");
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

    /**
     * Determine which question the user is currently answering by looking at the last bot message.
     */
    private int determineCurrentStep(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) return 0;
        ChatMessage lastBot = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            if ("BOT".equals(history.get(i).getSender())) {
                lastBot = history.get(i);
                break;
            }
        }
        if (lastBot == null) return 0;

        String text = lastBot.getMessage().toLowerCase();
        if (text.contains("10-digit mobile") || text.contains("mobile number") || text.contains("phone number")) {
            return 9; // Phone
        }
        if (text.contains("full name") || text.contains("what is your name")) {
            return 8; // Name
        }
        if (text.contains("quantity") || text.contains("minimum order is 2kg") || text.contains("at least 2kg")) {
            return 7; // Quantity
        }
        if (text.contains("special instructions") || text.contains("special requests")) {
            return 6; // Special instructions
        }
        if (text.contains("additional ingredients") || text.contains("extra ingredients")) {
            return 5; // Additional ingredients
        }
        if (text.contains("salt level")) {
            return 4; // Salt
        }
        if (text.contains("spice level")) {
            return 3; // Spice
        }
        if (text.contains("which oil") || text.contains("oil do you prefer") || text.contains("oil preference")) {
            return 2; // Oil
        }
        return 1; // Main ingredient
    }

    private CustomOrderRequest buildOrderFromHistory(String sessionId, List<ChatMessage> history,
                                                      String phone, Long userId) {
        String pickleType = "Mango (Avakaya)";
        String oil = "Chef's Choice";
        String spice = "Medium";
        String salt = "Medium";
        String additional = "None";
        String special = "None";
        String quantity = "2kg";
        String customerName = "Customer";

        for (int i = 0; i < history.size() - 1; i++) {
            ChatMessage bot = history.get(i);
            if ("BOT".equals(bot.getSender())) {
                String botText = bot.getMessage().toLowerCase();
                // Find next user message
                ChatMessage user = null;
                for (int j = i + 1; j < history.size(); j++) {
                    if ("USER".equals(history.get(j).getSender())) {
                        user = history.get(j);
                        break;
                    }
                }
                if (user != null) {
                    String msg = user.getMessage().trim();
                    if (botText.contains("main ingredient") && isValidIngredientInput(msg)) {
                        pickleType = resolveIngredient(msg);
                    } else if ((botText.contains("which oil") || botText.contains("oil do you prefer") || botText.contains("oil preference")) && isValidOilInput(msg)) {
                        oil = msg;
                    } else if (botText.contains("spice level") && isValidSpice(msg)) {
                        spice = msg;
                    } else if (botText.contains("salt level") && isValidSalt(msg)) {
                        salt = msg;
                    } else if (botText.contains("additional ingredients") || botText.contains("extra ingredients")) {
                        additional = msg;
                    } else if (botText.contains("special instructions") || botText.contains("special requests")) {
                        special = msg;
                    } else if (botText.contains("quantity") && isValidQuantity(msg)) {
                        quantity = msg;
                    } else if ((botText.contains("full name") || botText.contains("what is your name")) && !isPureGreeting(msg) && msg.length() >= 2) {
                        customerName = msg;
                    }
                }
            }
        }

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
