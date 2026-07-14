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
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(45000);

        this.restTemplate = new RestTemplate(factory);
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
                        "temperature", 0.7,
                        "maxOutputTokens", 2048,
                        "topP", 0.95,
                        "topK", 40
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String model = "gemini-2.5-flash";
        int maxRetries = Math.max(1, apiKeys.size());
        
        for (int i = 0; i < maxRetries; i++) {
            String currentKey = apiKeys.isEmpty() ? "" : apiKeys.get(Math.abs(currentKeyIndex.getAndIncrement()) % apiKeys.size());
            String url = apiUrl + model + ":generateContent?key=" + currentKey;

            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        new HttpEntity<>(body, headers),
                        Map.class
                );

                String text = extractTextSafe(response.getBody());

                if (text == null || text.isBlank()) {
                    throw new GeminiException(500, "Gemini returned empty response.");
                }

                return text.trim();

            } catch (HttpStatusCodeException e) {
                int status = e.getStatusCode().value();
                
                // If it's 429 Too Many Requests and we have more keys to try, continue to next iteration
                if (status == 429 && i < maxRetries - 1) {
                    log.warn("Gemini API Key overloaded (429). Retrying with another key...");
                    continue;
                }

                log.error("Gemini HTTP {}:\n{}", status, e.getResponseBodyAsString());
                throw new GeminiException(status, e.getResponseBodyAsString());

            } catch (Exception e) {
                log.error("Gemini API failed", e);
                throw new GeminiException(500, e.getMessage());
            }
        }
        
        throw new GeminiException(500, "All Gemini API keys failed or overloaded.");
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