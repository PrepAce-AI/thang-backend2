package backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Tích hợp Google Gemini API (gemini-1.5-flash).
 * Docs: https://ai.google.dev/api/generate-content
 */
@Slf4j
@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);

        this.restTemplate = new RestTemplate(factory);
    }
    /**
     * Gửi prompt tới Gemini và nhận về text response.
     * @param systemContext  Hướng dẫn cho AI (vai trò, giới hạn ngữ cảnh)
     * @param userPrompt     Câu hỏi / yêu cầu từ user
     * @return text response từ Gemini
     */
    public String ask(String systemContext, String userPrompt) {
        String fullPrompt = systemContext + "\n\n" + userPrompt;

        Map<String, Object> part = Map.of("text", fullPrompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of(
                "contents", List.of(content),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 2048
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = apiUrl + "?key=" + apiKey;

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            System.out.println(response.getBody());
            return extractText(response.getBody());
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
            throw new RuntimeException("AI service tạm thời không khả dụng. Vui lòng thử lại sau.");
        }
    }

//    @SuppressWarnings("unchecked")
//    private String extractText(Map<?, ?> body) {
//        if (body == null) return "Không nhận được phản hồi từ AI.";
//        try {
//            List<?> candidates = (List<?>) body.get("candidates");
//            if (candidates == null || candidates.isEmpty()) return "AI không trả về kết quả.";
//            Map<?, ?> candidate = (Map<?, ?>) candidates.get(0);
//            Map<?, ?> content = (Map<?, ?>) candidate.get("content");
//            List<?> parts = (List<?>) content.get("parts");
//            Map<?, ?> part = (Map<?, ?>) parts.get(0);
//            return (String) part.get("text");
//        } catch (Exception e) {
//            log.error("Failed to parse Gemini response: {}", e.getMessage());
//            return "Lỗi phân tích phản hồi AI.";
//        }
//    }
    @SuppressWarnings("unchecked")
    private String extractText(Map<?, ?> body) {

        System.out.println("FULL RESPONSE = " + body);

        try {

            List<Map<String, Object>> candidates =
                    (List<Map<String, Object>>) body.get("candidates");

            if (candidates == null || candidates.isEmpty()) {
                return "AI không trả về candidates.";
            }

            Map<String, Object> candidate = candidates.get(0);

            Map<String, Object> content =
                    (Map<String, Object>) candidate.get("content");

            if (content == null) {
                return "Không có content.";
            }

            List<Map<String, Object>> parts =
                    (List<Map<String, Object>>) content.get("parts");

            if (parts == null || parts.isEmpty()) {
                return "Không có parts.";
            }

            return String.valueOf(parts.get(0).get("text"));

        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi phân tích phản hồi AI.";
        }
    }
}
