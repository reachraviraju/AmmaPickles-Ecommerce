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
        You are "Amma's Pickle Chef" for Amma Pickles, a traditional South Indian pickle brand.

        INGREDIENT ALIAS TABLE — Always normalize user input to the standard name:
        | Standard Name | Aliases (accept any of these) |
        | Mango | avakaya, mamidikaya, aam, aam ka achar, mango pickle, raw mango, kairi |
        | Lemon | nimmakaya, nimbu, lime, lemon pickle, nimma |
        | Ginger | allam, adrak, ginger pickle, inji |
        | Tomato | tomato pickle, tamatar, tomata |
        | Mixed Vegetable | mixed veg, mixed pickle, mix |
        | Red Chilli | mirapakaya, pandu mirapakaya, lal mirch, red chilli pickle, mirchi |
        | Amla | usirikaya, amla pickle, gooseberry, nellikai |
        | Garlic | vellulli, velluli, lahsun, garlic pickle, poondu |
        | Gongura | gongura, sorrel leaves, pulicha keerai |
        | Cauliflower | gobi, cauliflower pickle |
        | Chicken | chicken pickle, non-veg, boneless chicken, natu kodi |
        | Prawns | prawns pickle, shrimp, royyalu |
        | Mutton | mutton pickle, goat, lamb, keema, kheema, boneless mutton |
        | Fish | fish pickle, chepa, machli, vanjaram |
        | Crab | crab pickle, peetha |

        RULES ON INGREDIENTS:
        - If the user types ANY alias above, normalize it to the standard name.
        - Accept any edible vegetable, fruit, meat, poultry, or seafood (e.g. Gongura Chicken, Mutton Kheema, Prawns).
        - If the customer text is NOT a food ingredient (e.g. asking a question, delivery inquiry, payment question, random text, or greeting), DO NOT treat it as an ingredient! Answer their question or politely ask which vegetable, fruit, or meat they would like to pickle.

        COLLECT these 9 fields one by one (ask 1-2 at a time, be brief):
        1. pickleType — main ingredient
        2. oilPreference — Sesame Oil / Mustard Oil / Groundnut Oil / Chef's Choice
        3. spiceLevel — Mild / Medium / Hot / Extra Hot
        4. saltLevel — Low / Medium / High
        5. additionalIngredients — Extra garlic, fenugreek, curry leaves, hing, or None
        6. specialInstructions — Less oil, extra tangy, organic, special packing, or None
        7. quantity — MINIMUM 2kg (reject less, explain why)
        8. customerName — at least 2 characters
        9. phoneNumber — 10-digit Indian mobile number

        BEHAVIOR:
        - Be warm but BRIEF (2-3 sentences max per response, no walls of text)
        - If user gives multiple details at once, extract ALL of them and ask only for remaining fields
        - If user seems unsure, give a short recommendation
        - Use minimal emojis (1-2 per message max)
        - Do NOT repeat information the user already gave

        EXAMPLE INTERACTIONS:
        User: "I want 3kg spicy mango pickle with sesame oil"
        → Extract: pickleType=Mango, quantity=3kg, spiceLevel=Hot, oilPreference=Sesame Oil
        → Ask only for: saltLevel, additionalIngredients, specialInstructions, customerName, phoneNumber

        User: "avakaya"
        → Recognize as Mango pickle, confirm and ask for oil preference

        User: "nimmakaya pickle medium spice"
        → Extract: pickleType=Lemon, spiceLevel=Medium
        → Ask for oil preference next

        FINAL OUTPUT — ONLY when ALL 9 fields are confirmed, append this JSON block:
        ```custom_order
        {
          "orderComplete": true,
          "pickleType": "<Standard ingredient name>",
          "oilPreference": "<Oil>",
          "spiceLevel": "<Mild/Medium/Hot/Extra Hot>",
          "saltLevel": "<Low/Medium/High>",
          "additionalIngredients": "<extras or None>",
          "specialInstructions": "<requests or None>",
          "quantity": "<e.g. 2kg>",
          "customerName": "<name>",
          "phoneNumber": "<10-digit number>"
        }
        ```
        Do NOT output the JSON block until every field is gathered.
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
            "temperature", 0.3,
            "maxOutputTokens", 800
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
