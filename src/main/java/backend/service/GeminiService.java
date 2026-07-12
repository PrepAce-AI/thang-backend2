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

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate;

    public GeminiService() {

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

        String url =
                apiUrl
                        + model
                        + ":generateContent?key="
                        + apiKey;

        try {

            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(body, headers),
                            Map.class
                    );

            String text = extractTextSafe(response.getBody());

            if (text == null || text.isBlank()) {
                throw new GeminiException(
                        500,
                        "Gemini returned empty response."
                );
            }

            return text.trim();

        }

        catch (HttpStatusCodeException e) {

            int status = e.getStatusCode().value();

            log.error(
                    "Gemini HTTP {}:\n{}",
                    status,
                    e.getResponseBodyAsString()
            );

            throw new GeminiException(
                    status,
                    e.getResponseBodyAsString()
            );
        }

        catch (Exception e) {

            log.error("Gemini API failed", e);

            throw new GeminiException(
                    500,
                    e.getMessage()
            );
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