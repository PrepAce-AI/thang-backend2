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
//        String model = "openai/gpt-oss-20b:free";
        String model = "gemini-2.5-flash";
        String url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                        + apiKey;
//        Map<String, Object> body = Map.of(
//                "model", model,
//                "messages", List.of(
//                        Map.of(
//                                "role", "system",
//                                "content", systemContext
//                        ),
//                        Map.of(
//                                "role", "user",
//                                "content", userPrompt
//                        )
//                )
//        );
        Map<String,Object> body = Map.of(
                "systemInstruction",
                Map.of(
                        "parts",
                        List.of(
                                Map.of(
                                        "text",
                                        systemContext
                                )
                        )
                ),
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                userPrompt
                                        )
                                )
                        )
                )
        );


        HttpHeaders headers = new HttpHeaders();
        //headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

//        try {
//            System.out.println("========== OPENROUTER ==========");
//            System.out.println(apiUrl);
//            System.out.println("Model = " + model);
//            ResponseEntity<Map> response =
//                    restTemplate.exchange(
//                            apiUrl,
//                            HttpMethod.POST,
//                            new HttpEntity<>(body, headers),
//                            Map.class
//                    );
//            String text = extractTextSafe(response.getBody());
//            if (text == null || text.isBlank()) {
//                throw new GeminiException(
//                        500,
//                        "OpenRouter returned empty response."
//                );
//            }
//            return text.trim();
//        }
//
//        catch (HttpStatusCodeException e) {
//            int status = e.getStatusCode().value();
//            log.error(
//                    "OpenRouter HTTP {}:\n{}",
//                    status,
//                    e.getResponseBodyAsString()
//            );
//            throw new GeminiException(
//                    status,
//                    e.getResponseBodyAsString()
//            );
//        }
//
//        catch (Exception e) {
//
//            log.error("OpenRouter API failed", e);
//
//            throw new GeminiException(
//                    500,
//                    e.getMessage()
//            );
//        }
        try {
            System.out.println("====================");
            System.out.println("CALL GEMINI");
            System.out.println(url);
            ResponseEntity<Map> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(
                                    body,
                                    headers
                            ),
                            Map.class
                    );
            return extractTextSafe(response.getBody());
        }
        catch(HttpStatusCodeException e){
            log.error(
                    "Gemini HTTP {} : {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );
            throw new GeminiException(
                    e.getStatusCode().value(),
                    e.getResponseBodyAsString()
            );
        }

        catch(Exception e){
            log.error(
                    "Gemini failed",
                    e
            );
            throw new GeminiException(
                    500,
                    e.getMessage()
            );
        }
    }

    /**
     * Parse OpenRouter response
     */
    private String extractTextSafe(Map<?, ?> body) {
//        try {
//            if (body == null) return null;
//
//            List<?> choices = (List<?>) body.get("choices");
//
//            if (choices == null || choices.isEmpty())
//                return null;
//            Map<?, ?> choice = (Map<?, ?>) choices.get(0);
//            Map<?, ?> message =
//                    (Map<?, ?>) choice.get("message");
//            if (message == null)
//                return null;
//            Object content = message.get("content");
//            if (content == null)
//                return null;
//            return content.toString();
//        }
//        catch (Exception e) {
//            log.error("Parse OpenRouter response failed", e);
//            return null;
//        }
//    }

        try {
            List<?> candidates =
                    (List<?>) body.get("candidates");
            if(candidates == null ||
                    candidates.isEmpty())
                return null;
            Map<?,?> candidate =
                    (Map<?,?>) candidates.get(0);
            Map<?,?> content =
                    (Map<?,?>) candidate.get("content");
            List<?> parts =
                    (List<?>) content.get("parts");
            Map<?,?> part =
                    (Map<?,?>) parts.get(0);
            return part.get("text")
                    .toString();
        }
        catch(Exception e){
            log.error(
                    "Parse Gemini response failed",
                    e
            );
            return null;
        }
    }

}