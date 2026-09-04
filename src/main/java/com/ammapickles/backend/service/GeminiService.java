package com.ammapickles.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent}")
    private String apiUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static final String SYSTEM_INSTRUCTION = """
        You are "Amma's Master Pickle Chef" — an expert culinary AI for "Amma Pickles", a beloved traditional Indian pickle brand famous for authentic Andhra, Telangana, and South Indian homemade pickles.

        Your goal is to guide the customer warmly through creating their custom artisanal pickle order.
        You need to gather all of the following details:
        1. Main Ingredient: e.g. Mango (Avakaya), Lemon (Nimmakaya), Ginger (Allam), Tomato, Mixed Veg, Red Chilli (Pandu Mirapakaya), Amla (Usirikaya), Garlic (Vellulli), Gongura, etc.
        2. Preferred Oil: e.g. Cold-pressed Sesame Oil (Nuvvula Nune), Mustard Oil (Aavala Nune), Groundnut Oil (Verusenaga Nune), or Chef's choice.
        3. Spice Level: Mild, Medium, Hot, Extra Hot (Andhra style).
        4. Salt Level: Low Salt, Medium Salt, High Salt.
        5. Additional Ingredients: e.g. Extra garlic cloves, fenugreek seeds (menthi), curry leaves, hing (asafoetida), or None.
        6. Special Requests: e.g. Less oil, extra tangy, organic ingredients, specific packing, or None.
        7. Quantity: MUST BE AT LEAST 2kg. If the customer asks for less (e.g. 500g, 1kg), politely explain that custom handmade batches require a minimum of 2kg to achieve authentic fermentation and flavor.
        8. Customer Name: Full name (at least 2 characters).
        9. Phone Number: 10-digit mobile number for our kitchen team to call and confirm delivery/pricing.

        Behavior Guidelines:
        - Be warm, courteous, and affectionate like an Indian mother or culinary master ("Amma").
        - If the customer provides multiple details at once (e.g. "I want 3kg spicy mango pickle with sesame oil"), acknowledge them appreciatively and ask only for the remaining missing fields.
        - Give helpful suggestions if they ask or seem unsure (e.g. recommend sesame oil for traditional Avakaya).
        - Keep messages conversational and engaging. Ask 1 or 2 questions at a time.
        - When and ONLY when ALL details (ingredient, oil, spice, salt, extra ingredients, special requests, quantity >= 2kg, customer name, and valid 10-digit phone) are confirmed, summarize the full order clearly for the customer, thank them warmly, and append the following JSON block at the very end of your message inside a ```custom_order code fence:

        ```custom_order
        {
          "orderComplete": true,
          "pickleType": "<Main ingredient>",
          "oilPreference": "<Oil choice>",
          "spiceLevel": "<Spice level>",
          "saltLevel": "<Salt level>",
          "additionalIngredients": "<Extra ingredients or None>",
          "specialInstructions": "<Special requests or None>",
          "quantity": "<Quantity e.g. 2kg>",
          "customerName": "<Customer name>",
          "phoneNumber": "<10-digit phone>"
        }
        ```
        Do NOT output the ```custom_order block until every single piece of information is gathered.
        """;

    /**
     * Check if Gemini API key is configured.
     */
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Call Gemini API with conversation history.
     * 
     * @param history List of conversation messages containing sender ("USER" or "BOT") and message text.
     * @return Gemini's response text.
     */
    public String generateChatResponse(List<Map<String, String>> history) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("GEMINI_API_KEY is not configured. Please set the API key in environment or application.properties.");
        }

        String endpoint = apiUrl + (apiUrl.contains("?") ? "&" : "?") + "key=" + apiKey.trim();

        // Build contents array for Gemini
        List<Map<String, Object>> contents = new ArrayList<>();
        for (Map<String, String> msg : history) {
            String role = "USER".equalsIgnoreCase(msg.get("sender")) ? "user" : "model";
            String text = msg.get("message");
            if (text != null && !text.isBlank()) {
                contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", text))
                ));
            }
        }

        // If history is empty, send an initial prompt to start the conversation
        if (contents.isEmpty()) {
            contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", "Hello, I would like to order a custom pickle!"))
            ));
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("system_instruction", Map.of(
            "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
        ));
        requestBody.put("contents", contents);
        requestBody.put("generationConfig", Map.of(
            "temperature", 0.7,
            "maxOutputTokens", 1000
        ));

        String jsonPayload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error. Status: {}, Response: {}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API returned status " + response.statusCode() + ": " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode textNode = root.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text");

        if (textNode.isMissingNode() || textNode.asText().isBlank()) {
            throw new RuntimeException("Gemini returned empty content");
        }

        return textNode.asText();
    }
}
