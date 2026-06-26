package backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Tích hợp Google Gemini API - Đã cập nhật model mới nhất 2026
 */
@Service
@Slf4j
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;   // Ví dụ: https://generativelanguage.googleapis.com/v1beta/models/

    private final RestTemplate restTemplate;

    public GeminiService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(45000);
        this.restTemplate = new RestTemplate(factory);
    }

    public String ask(String systemContext, String userPrompt) {

        String fullPrompt = systemContext + "\n\n" + userPrompt;

        Map<String, Object> part = Map.of("text", fullPrompt);
        Map<String, Object> content = Map.of("parts", List.of(part));

        Map<String, Object> body = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048,     // Tăng nhẹ cho câu trả lời tốt hơn
                        "topP", 0.95,
                        "topK", 40
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // ✅ Model mới nhất & ổn định (2026)
        String modelName = "gemini-2.5-flash";           // Hoặc "gemini-flash-latest"
        String url = apiUrl + modelName + ":generateContent?key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            Map<?, ?> bodyRes = response.getBody();
            String text = extractTextSafe(bodyRes);

            if (text == null || text.isBlank()) {
                return fallback("Empty response from Gemini");
            }

            return text.trim();

        } catch (Exception e) {
            log.error("Gemini API failed", e);
            return fallback(e.getMessage());
        }
    }

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

            Map<?, ?> part = (Map<?, ?>) parts.get(0);
            Object text = part.get("text");
            return text != null ? text.toString() : null;

        } catch (Exception e) {
            log.error("Parse Gemini response failed", e);
            return null;
        }
    }

    private String fallback(String reason) {
        log.warn("Gemini fallback triggered: {}", reason);
        return "Xin lỗi, AI đang bận. Bạn thử hỏi lại nhé! 💪";
    }
}