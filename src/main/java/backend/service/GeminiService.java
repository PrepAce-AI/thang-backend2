package backend.service;

import backend.exceptions.GeminiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Google Gemini Service
 * Stable version
 */
@Service
@Slf4j
public class GeminiService {

    private List<String> apiKeys = new java.util.ArrayList<>();
    private java.util.concurrent.atomic.AtomicInteger currentKeyIndex = new java.util.concurrent.atomic.AtomicInteger(0);

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService(@Value("${gemini.api.keys:${gemini.api.key:}}") String keysString) {
        if (keysString != null && !keysString.trim().isEmpty()) {
            String[] keys = keysString.split(",");
            for (String k : keys) {
                if (!k.trim().isEmpty()) {
                    apiKeys.add(k.trim());
                }
            }
        }
        if (apiKeys.isEmpty()) {
            log.warn("No Gemini API keys configured!");
        }

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(45000); // 45s to allow parallel queueing

        this.restTemplate = new RestTemplate(factory);
        
        // Asynchronously fetch and log available models to diagnose 404 errors
        new Thread(this::listModels).start();
    }

    private void listModels() {
        if (apiKeys.isEmpty()) return;
        try {
            String currentKey = apiKeys.get(0);
            String url = "https://generativelanguage.googleapis.com/v1beta/models?key=" + currentKey;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            log.info("AVAILABLE GEMINI MODELS:\n{}", response.getBody());
        } catch (Exception e) {
            log.error("Failed to list models", e);
        }
    }

    public String ask(String systemContext, String userPrompt) {

        String fullPrompt = systemContext + "\n\n" + userPrompt;

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of("text", fullPrompt)
                                )
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", 0.3,
                        "topP", 0.9,
                        "topK", 40,
                        "maxOutputTokens", 8192
                )
        );

        String model = "gemini-flash-latest";
        
        if (apiKeys.isEmpty()) {
            throw new GeminiException(500, "No API keys configured");
        }
        
        String url = apiUrl + model + ":generateContent";

        // To make it ultra-fast and bypass Google's queue, we blast ALL keys in parallel
        // and just take the answer from whichever one finishes FIRST.
        java.util.List<java.util.concurrent.CompletableFuture<String>> futures = new java.util.ArrayList<>();
        
        for (String key : apiKeys) {
            futures.add(java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                if (!key.isEmpty()) {
                    headers.set("x-goog-api-key", key);
                }

                try {
                    ResponseEntity<Map> response = restTemplate.exchange(
                            url, HttpMethod.POST, new HttpEntity<>(body, headers), Map.class
                    );
                    String text = extractTextSafe(response.getBody());
                    if (text == null || text.isBlank()) throw new RuntimeException("Empty response");
                    return text.trim();
                } catch (org.springframework.web.client.HttpStatusCodeException e) {
                    log.error("Gemini HTTP Error (Parallel): {}", e.getResponseBodyAsString());
                    throw new RuntimeException("HTTP Error: " + e.getStatusCode());
                } catch (Exception e) {
                    log.error("Gemini Generic Error (Parallel): {}", e.getMessage());
                    throw new RuntimeException(e);
                }
            }));
        }

        try {
            // We need the first SUCCESSFUL future, not just the first to complete (which might be an error).
            // A simple way to get first success: wait for all, but return as soon as one succeeds.
            java.util.concurrent.CompletableFuture<String> firstSuccess = new java.util.concurrent.CompletableFuture<>();
            for (java.util.concurrent.CompletableFuture<String> f : futures) {
                f.thenAccept(result -> firstSuccess.complete(result));
            }
            // If all fail, complete exceptionally
            java.util.concurrent.CompletableFuture.allOf(futures.toArray(new java.util.concurrent.CompletableFuture[0]))
                .exceptionally(ex -> {
                    if (!firstSuccess.isDone()) firstSuccess.completeExceptionally(new GeminiException(500, "All keys failed or overloaded"));
                    return null;
                });
                
            return firstSuccess.join();
        } catch (Exception e) {
            log.error("All Gemini API keys failed", e);
            throw new GeminiException(500, "All Gemini API keys failed or overloaded.");
        }
    }

    /**
     * Extract generated text safely
     */
    private String extractTextSafe(Map<?, ?> body) {

        try {

            if (body == null) return null;

            List<?> candidates = (List<?>) body.get("candidates");
            if (candidates == null || candidates.isEmpty()) return null;

            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);

            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
            if (content == null) return null;

            List<?> parts = (List<?>) content.get("parts");
            if (parts == null || parts.isEmpty()) return null;

            StringBuilder sb = new StringBuilder();

            for (Object obj : parts) {
                Map<?, ?> part = (Map<?, ?>) obj;

                Object text = part.get("text");
                if (text != null) {
                    sb.append(text);
                }
            }

            return sb.toString();

        } catch (Exception e) {

            log.error("Parse Gemini response failed", e);
            return null;
        }
    }

}