package backend.service;

import backend.dto.request.ChatRequest;
import backend.dto.response.AdaptivePathResponse;
import backend.dto.response.ChatResponse;
import backend.dto.response.UniversityAdvisingResponse;
import backend.dto.response.UniversitySuggestion;
import backend.entity.AIChatHistory;
import backend.entity.QuizAttempt;
import backend.exceptions.GeminiException;
import backend.repository.AIChatHistoryRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.QuizAttemptRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * UC-26: Consult AI Chatbot  (Gemini integration)
 * UC-27: Adaptive Path Generation
 * UC-28: AI Gap Diagnosis
 * UC-29: AI Score Forecasting
 * UC-30: AI University Advising
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIService {

    private final GeminiService geminiService;
    private final AIChatHistoryRepository chatHistoryRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_CONTEXT =
            "Bạn là trợ lý AI của nền tảng PrepAce — hệ thống luyện thi THPT Quốc gia Việt Nam. "
          + "Hãy trả lời bằng tiếng Việt, ngắn gọn, chính xác và phù hợp với học sinh cấp 3. "
          + "Chỉ hỗ trợ các vấn đề liên quan đến học tập, ôn thi và tư vấn hướng nghiệp đại học. "
          + "Không trả lời các câu hỏi ngoài phạm vi giáo dục."
          + "Có kiến thức chuyên sâu về các đời sống học sinh, sinh viên"
            + "CHỈ TRẢ VỀ JSON HỢP LỆ.\n" +
                    "KHÔNG markdown.\n" +
                    "KHÔNG ```.\n" +
                    "KHÔNG giải thích.\n" +
                    "KHÔNG xuống dòng ngoài JSON.\n" +
                    "Nếu không chắc chắn, vẫn phải trả JSON hợp lệ.";

    private static final String CHAT_CONTEXT = """
        Bạn là PrepAce AI - trợ lý học tập thông minh dành cho học sinh THPT Việt Nam.
        
        NHIỆM VỤ
        - Hỗ trợ học sinh học tập.
        - Giải thích kiến thức.
        - Giải bài tập.
        - Đưa ra phương pháp học.
        - Tư vấn ôn thi THPT Quốc Gia.
        - Tư vấn hướng nghiệp.
        - Chỉ trả lời các chủ đề liên quan giáo dục.
        
        NGUYÊN TẮC
        - Luôn trả lời bằng tiếng Việt.
        - Giải thích rõ ràng, dễ hiểu.
        - Không lan man.
        - Không trả lời chủ đề chính trị, bạo lực, người lớn, hack, vi phạm pháp luật.
        - Nếu câu hỏi ngoài phạm vi giáo dục, hãy lịch sự từ chối.
        
        ĐỊNH DẠNG BẮT BUỘC
        
        {
          "response":"Nội dung trả lời"
        }
        
        KHÔNG markdown.
        
        KHÔNG ```.
        
        KHÔNG giải thích ngoài JSON.
        
        KHÔNG đổi tên field response.
        """;

    private static final String SUMMARY_CONTEXT = """
        Bạn là chuyên gia biên soạn tài liệu ôn thi THPT Quốc gia Việt Nam của PrepAce.
        
        NHIỆM VỤ
        - Tóm tắt kiến thức theo chương trình THPT Việt Nam.
        - Viết dễ hiểu cho học sinh lớp 12.
        - Chỉ giữ lại những ý quan trọng.
        - Nếu có công thức, phải trình bày rõ ràng.
        - Nếu có mẹo ghi nhớ hoặc lưu ý, hãy thêm ở cuối.
        
        QUY TẮC ĐỊNH DẠNG
        
        BẮT BUỘC trả về Markdown hợp lệ.
        
        Sử dụng:
        
        # Tiêu đề chính
        
        ## Các mục kiến thức
        
        ### Tiểu mục (nếu cần)
        
        - Bullet list
        
        **In đậm** cho khái niệm quan trọng.
        
        Công thức toán phải đặt trong:
        
        $$
        ...
        $$
        
        hoặc
        
        $...$
        
        Không sử dụng HTML.
        
        Không sử dụng JSON.
        
        Không sử dụng ```.
        
        Không thêm lời mở đầu như:
        "Đây là bản tóm tắt..."
        "Chắc chắn rồi..."
        
        Bắt đầu ngay bằng tiêu đề.
        
        CẤU TRÚC MONG MUỐN
        
        # <Tên chủ đề>
        
        ## 1. Định nghĩa
        
        - ...
        
        ## 2. Công thức
        
        $$
        ...
        $$
        
        ## 3. Tính chất
        
        - ...
        
        ## 4. Lưu ý
        
        - ...
        
        Nếu chủ đề không có công thức thì bỏ phần Công thức.
        
        Nếu chủ đề không thuộc chương trình THPT thì vẫn tóm tắt ngắn gọn theo kiến thức chính xác.
        """;

    private static final String ADAPTIVE_CONTEXT = """
        Bạn là AI Learning Coach của PrepAce.
        
        Hãy phân tích năng lực học sinh dựa trên dữ liệu đầu vào.
        
        Đưa ra:
        
        - Điểm mạnh
        - Điểm yếu
        - Chủ đề cần học
        - Độ ưu tiên
        - Kế hoạch học
        
        Trả về JSON.
        
        Schema:
        
        {
          "overallLevel":"...",
          "strengths":[
            "..."
          ],
          "weaknesses":[
            "..."
          ],
          "learningPath":[
            {
              "topic":"...",
              "priority":"HIGH | MEDIUM | LOW",
              "reason":"..."
            }
          ],
          "tips":[
            "..."
          ]
        }
        
        Không markdown.
        
        Không ```.
        
        Không thêm text ngoài JSON.
        """;

    private static final String FORECAST_CONTEXT = """
        Bạn là AI Prediction Engine của PrepAce.
        
        Dựa trên:
        
        - Điểm trung bình.
        - Xu hướng tăng giảm.
        - Kết quả gần đây.
        
        Hãy dự đoán điểm thi THPT Quốc Gia.
        
        Schema:
        
        {
          "predictedScore":0,
          "confidence":0,
          "analysis":"...",
          "improvements":[
            "..."
          ]
        }
        
        Trong đó
        
        predictedScore
        0 -> 30
        
        confidence
        0 -> 100
        
        analysis
        Giải thích ngắn.
        
        improvements
        Các lời khuyên.
        
        Không markdown.
        
        Không ```.
        
        Không thêm text.
        """;

    private static final String UNIVERSITY_CONTEXT = """
        Bạn là chuyên gia tư vấn tuyển sinh đại học Việt Nam năm 2026.
        
        NHIỆM VỤ
        
        Dựa trên:
        
        - Điểm dự đoán.
        - Khối xét tuyển.
        - Nguyện vọng.
        
        Hãy đề xuất trường phù hợp.
        
        Schema:
        
        {
          "summary":"...",
          "suggestions":[
            {
              "universityName":"...",
              "major":"...",
              "admissionScore":"...",
              "matchScore":95,
              "reason":"..."
            }
          ]
        }
        
        QUY TẮC
        
        - MatchScore từ 0-100.
        - Sắp xếp từ cao xuống thấp.
        - Chỉ đề xuất trường có thật tại Việt Nam.
        - Không bịa tên trường.
        - Không markdown.
        - Không ```.
        
        Chỉ trả JSON.
        """;
    // ─── UC-26: AI Chatbot ───────────────────────────────────────────────────────

    @Transactional
    public ChatResponse chat(Integer studentId, ChatRequest request) {

        String contextualPrompt = request.getSubject() != null
                ? "[Môn: " + request.getSubject() + "] " + request.getMessage()
                : request.getMessage();

        String aiResponse;

        try {
            String question = request.getMessage().toLowerCase();
            String context;
            if (question.contains("tóm tắt")
                    || question.contains("tóm lược")
                    || question.contains("ghi chú")
                    || question.contains("mindmap")) {
                context = SYSTEM_CONTEXT + SUMMARY_CONTEXT;
            } else {
                context = SYSTEM_CONTEXT + CHAT_CONTEXT;
            }

            aiResponse = geminiService.ask(context, request.getMessage());

            if (aiResponse == null || aiResponse.isBlank()) {
                aiResponse = "Xin lỗi, AI hiện không phản hồi.";
            }
            else if (aiResponse.trim().startsWith("{")) {

                JsonNode json = safeJsonParse(aiResponse);

                if (json != null) {

                    if (json.has("response"))
                        aiResponse = json.get("response").asText();

                    else if (json.has("answer"))
                        aiResponse = json.get("answer").asText();

                    else if (json.has("message"))
                        aiResponse = json.get("message").asText();

                    else if (json.has("text"))
                        aiResponse = json.get("text").asText();
                }
            }

        } catch (GeminiException e){
            switch (e.getStatusCode()) {
                case 429 ->
                        aiResponse =
                                "🚦 AI đang quá tải hoặc đã hết quota hôm nay. Vui lòng thử lại sau vài phút.";

                case 403 ->
                        aiResponse =
                                "🔑 API Key của hệ thống AI không hợp lệ hoặc đã bị khóa.";

                case 401 ->
                        aiResponse =
                                "🔒 Không thể xác thực với dịch vụ AI.";

                default ->
                        aiResponse =
                                "❌ Không thể kết nối tới AI. Vui lòng thử lại sau.";
            }
        }catch (Exception e) {
            log.error("Gemini error", e);

            // KHÔNG ghi DB fallback im lặng
            aiResponse = "Xin lỗi, AI đang quá tải. Vui lòng thử lại sau.";
        }

        AIChatHistory record = new AIChatHistory();
        record.setStudentId(studentId);
        record.setQuestion(request.getMessage());
        record.setAiResponse(aiResponse);
        record.setCreatedAt(new Date());
        record.setRequestType("CHAT");

        AIChatHistory saved = chatHistoryRepository.save(record);

        log.info("AI Chat SUCCESS — studentId={}, chars={}", studentId, aiResponse.length());

        return ChatResponse.builder()
                .chatId(saved.getChatId())
                .question(request.getMessage())
                .aiResponse(aiResponse)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // ─── UC-28: AI Gap Diagnosis ─────────────────────────────────────────────────

    @Transactional
    public AdaptivePathResponse diagnoseGaps(Integer studentId) {
        List<QuizAttempt> attempts = quizAttemptRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);

        if (attempts.isEmpty()) {
            return AdaptivePathResponse.builder()
                    .studentId(studentId)
                    .averageScore(0.0)
                    .overallLevel("Chưa có dữ liệu")
                    .gaps(List.of())
                    .recommendedPath(List.of())
                    .cognitiveChart(Map.of())
                    .build();
        }

        double avgScore = attempts.stream()
                .filter(a -> a.getScore() != null)
                .mapToDouble(QuizAttempt::getScore)
                .average()
                .orElse(0.0);

        // Phân tích cognitive level
        Map<Integer, List<QuizAttempt>> byLevel = attempts.stream()
                .filter(a -> a.getQuiz() != null)
                .collect(Collectors.groupingBy(
                        a -> a.getQuiz().getQuizId() % 4 + 1  // Mock phân tầng; thực tế lấy từ Question.cognitiveLevel
                ));

        Map<String, Double> cognitiveChart = new LinkedHashMap<>();
        cognitiveChart.put("Nhận biết", calcRate(byLevel.get(1)));
        cognitiveChart.put("Thông hiểu", calcRate(byLevel.get(2)));
        cognitiveChart.put("Vận dụng", calcRate(byLevel.get(3)));
        cognitiveChart.put("Vận dụng cao", calcRate(byLevel.get(4)));

        // Dùng Gemini phân tích lỗ hổng
        String prompt = buildGapDiagnosisPrompt(avgScore, cognitiveChart);
        String aiAnalysis = geminiService.ask(SYSTEM_CONTEXT, prompt);

        // Lưu lịch sử
        saveAIHistory(studentId, "Phân tích lỗ hổng kiến thức", aiAnalysis, "GAP_DIAGNOSIS");

        List<AdaptivePathResponse.GapItem> gaps = extractGapsFromAnalysis(cognitiveChart);

        log.info("Gap Diagnosis — studentId={}, avgScore={}", studentId, avgScore);

        return AdaptivePathResponse.builder()
                .studentId(studentId)
                .averageScore(Math.round(avgScore * 10.0) / 10.0)
                .overallLevel(classifyLevel(avgScore / 10 * 100))
                .gaps(gaps)
                .recommendedPath(buildRecommendedPath(gaps))
                .cognitiveChart(cognitiveChart)
                .strengths(identifyStrengths(cognitiveChart))
                .build();
    }

    // ─── UC-27: Adaptive Path Generation ────────────────────────────────────────

    @Transactional
    public AdaptivePathResponse generateAdaptivePath(Integer studentId) {
        // Gọi diagnoseGaps để lấy dữ liệu cơ bản
        AdaptivePathResponse base = diagnoseGaps(studentId);

        // Gemini gợi ý lộ trình học cụ thể
        String prompt = "Dựa trên dữ liệu năng lực: điểm trung bình " + base.getAverageScore()
                + "/10, mức độ: " + base.getOverallLevel()
                + ". Hãy đề xuất lộ trình học tập 30 ngày chi tiết cho học sinh luyện thi THPT QG.";
        String aiPath = geminiService.ask(ADAPTIVE_CONTEXT, prompt);

        saveAIHistory(studentId, "Tạo lộ trình học thích ứng", aiPath, "ADAPTIVE_PATH");

        log.info("Adaptive Path — studentId={}", studentId);
        return base;
    }

    // ─── UC-29: AI Score Forecasting ────────────────────────────────────────────

    @Transactional
    public ChatResponse forecastScore(Integer studentId) {
        Double avgScore = quizAttemptRepository.findAverageScoreByStudentId(studentId).orElse(0.0);
        List<QuizAttempt> recent = quizAttemptRepository
                .findByStudentIdOrderBySubmittedAtDesc(studentId)
                .stream().limit(5).toList();

        double trend = calculateTrend(recent);

        String prompt = String.format(
                "Học sinh có điểm trung bình %s/10 qua các lần thi. Xu hướng gần đây %s. "
              + "Hãy dự đoán điểm thi THPT QG và đưa ra lời khuyên cụ thể để cải thiện.",
                String.format("%.1f", avgScore),
                trend > 0 ? "tăng +" + String.format("%.1f", trend) : "giảm " + String.format("%.1f", trend)
        );

        String forecast = geminiService.ask(FORECAST_CONTEXT, prompt);
        saveAIHistory(studentId, "Dự đoán điểm thi THPT QG", forecast, "SCORE_FORECAST");

        log.info("Score Forecast — studentId={}, avg={}", studentId, avgScore);

        return ChatResponse.builder()
                .question("Dự đoán điểm thi THPT QG của tôi")
                .aiResponse(forecast)
                .createdAt(new Date())
                .build();
    }

    // ─── UC-30: AI University Advising ──────────────────────────────────────────

    @Transactional
    public UniversityAdvisingResponse getUniversityAdvising(Integer studentId, String block) {

        Double avgScore = quizAttemptRepository
                .findAverageScoreByStudentId(studentId)
                .orElse(0.0);

        boolean hasData = avgScore > 0;

        // ─────────────────────────────
        // 1. AI PROMPT (structured JSON yêu cầu Gemini)
        // ─────────────────────────────
        String prompt = """
            You are a JSON API.
            
            ABSOLUTE RULES:
            - Output ONLY valid JSON
            - No markdown
            - No explanation
            - No extra text
            - Using Vietnamese
            - Must be complete JSON (never cut)
            - If unsure, still return valid JSON
            
            Return format exactly:
            
            {
              "suggestions": [
                {
                  "universityName": "string",
                  "major": "string",
                  "admissionScore": "string",
                  "reason": "string",
                  "matchScore": 0.0
                }
              ],
              "summary": "string"
            }
            
            User data:
            - avgScore: %.1f
            - block: %s
            """.formatted(avgScore, block);

        String aiResponse = geminiService.ask(UNIVERSITY_CONTEXT, prompt);
        if (aiResponse == null || aiResponse.length() < 10) {
            return UniversityAdvisingResponse.builder()
                    .hasData(hasData)
                    .block(block)
                    .predictedScore(avgScore)
                    .summary("AI không trả dữ liệu hợp lệ")
                    .suggestions(List.of())
                    .build();
        }

        saveAIHistory(studentId,
                "AI University Advising",
                aiResponse,
                "UNIVERSITY_ADVISE");

        // ─────────────────────────────
        // 2. PARSE JSON (quan trọng)
        // ─────────────────────────────
        List<UniversitySuggestion> suggestions = parseSuggestions(aiResponse);

        // ─────────────────────────────
        // 3. SORT by matchScore (AI + backend hybrid)
        // ─────────────────────────────
        suggestions = suggestions.stream()
                .sorted(Comparator.comparing(UniversitySuggestion::getMatchScore)
                        .reversed())
                .toList();

        // ─────────────────────────────
        // 4. SUMMARY extract
        // ─────────────────────────────
        String summary = extractSummary(aiResponse);

        return UniversityAdvisingResponse.builder()
                .hasData(hasData)
                .block(block)
                .predictedScore(avgScore)
                .summary(summary)
                .suggestions(suggestions)
                .build();
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    private double calcRate(List<QuizAttempt> attempts) {
        if (attempts == null || attempts.isEmpty()) return 0.0;
        return attempts.stream()
                .filter(a -> a.getTotalQuestions() != null && a.getTotalQuestions() > 0)
                .mapToDouble(a -> (double) a.getCorrectCount() / a.getTotalQuestions() * 100)
                .average()
                .orElse(0.0);
    }

    private double calculateTrend(List<QuizAttempt> recent) {
        if (recent.size() < 2) return 0.0;
        double first = recent.get(recent.size() - 1).getScore() != null ? recent.get(recent.size() - 1).getScore() : 0;
        double last = recent.get(0).getScore() != null ? recent.get(0).getScore() : 0;
        return last - first;
    }

    private String classifyLevel(double percentage) {
        if (percentage >= 80) return "Giỏi";
        if (percentage >= 65) return "Khá";
        if (percentage >= 50) return "Trung bình";
        return "Yếu";
    }

    private List<String> identifyStrengths(Map<String, Double> chart) {
        return chart.entrySet().stream()
                .filter(e -> e.getValue() >= 70)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<AdaptivePathResponse.GapItem> extractGapsFromAnalysis(Map<String, Double> chart) {
        return chart.entrySet().stream()
                .filter(e -> e.getValue() < 65)
                .map(e -> AdaptivePathResponse.GapItem.builder()
                        .topic(e.getKey())
                        .correctRate(Math.round(e.getValue() * 10.0) / 10.0)
                        .recommendation("Cần ôn tập thêm phần " + e.getKey())
                        .build())
                .toList();
    }

    private List<AdaptivePathResponse.LearningPathItem> buildRecommendedPath(
            List<AdaptivePathResponse.GapItem> gaps) {
        return gaps.stream()
                .map(g -> AdaptivePathResponse.LearningPathItem.builder()
                        .priority(g.getCorrectRate() < 40 ? "HIGH" : "MEDIUM")
                        .reason("Tỉ lệ đúng " + g.getCorrectRate() + "% — cần củng cố")
                        .lessonTitle("Ôn tập " + g.getTopic())
                        .build())
                .toList();
    }

    private String buildGapDiagnosisPrompt(double avgScore, Map<String, Double> chart) {
        StringBuilder sb = new StringBuilder();
        sb.append("Phân tích năng lực học sinh THPT:\n");
        sb.append("- Điểm trung bình: ").append(String.format("%.1f", avgScore)).append("/10\n");
        chart.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(String.format("%.0f", v)).append("%\n"));
        sb.append("\nHãy chỉ ra lỗ hổng kiến thức cụ thể và đề xuất cách khắc phục.");
        return sb.toString();
    }

    private void saveAIHistory(Integer studentId, String question, String response, String type) {
        AIChatHistory h = new AIChatHistory();
        h.setStudentId(studentId);
        h.setQuestion(question);
        h.setAiResponse(response);
        h.setCreatedAt(new Date());
        h.setRequestType(type);
        chatHistoryRepository.save(h);
    }

    public Page<ChatResponse> getChatHistory(Integer studentId, int page, int size) {
        Page<AIChatHistory> historyPage =
                chatHistoryRepository.findByStudentIdOrderByCreatedAtDesc(
                        studentId,
                        PageRequest.of(page, size)
                );
        return historyPage.map(h -> ChatResponse.builder()
                .chatId(h.getChatId())
                .question(h.getQuestion())
                .aiResponse(h.getAiResponse())
                .createdAt(h.getCreatedAt())
                .build()
        );
    }

    // --------- Parse Json --------
    private boolean isValidJson(String json) {
        return json != null
                && json.trim().startsWith("{")
                && json.trim().endsWith("}");
    }

    private String cleanJson(String aiResponse) {
        if (aiResponse == null) return "";

        return aiResponse
                .replaceAll("(?s)```json", "")
                .replaceAll("(?s)```", "")
                .replaceAll("[^\\x20-\\x7E\\n\\r\\t\u00A0-\\uFFFF]", "") // loại ký tự rác
                .trim();
    }

    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull()
                ? node.get(field).asText()
                : "";
    }

    private JsonNode safeParse(String json) {
        try {
            String cleaned = cleanJson(json);

            if (!isValidJson(cleaned)) {
                log.error("AI returned invalid JSON:\n{}", cleaned);
                return null;
            }

            return new ObjectMapper().readTree(cleaned);

        } catch (Exception e) {
            log.error("Invalid JSON from AI:\n{}", json, e);
            return null;
        }
    }

    private JsonNode safeJsonParse(String aiResponse) {
        try {
            if (aiResponse == null) return null;

            String cleaned = aiResponse
                    .replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            return objectMapper.readTree(cleaned);

        } catch (Exception e) {
            log.error("JSON parse failed. Raw AI:\n{}", aiResponse, e);
            return null;
        }
    }
    private List<UniversitySuggestion> parseSuggestions(String aiResponse) {
        try {
            JsonNode root = safeJsonParse(aiResponse);

            if (root == null || !root.has("suggestions")) {
                return new ArrayList<>();
            }

            return objectMapper.convertValue(
                    root.get("suggestions"),
                    new com.fasterxml.jackson.core.type.TypeReference<List<UniversitySuggestion>>() {}
            );

        } catch (Exception e) {
            log.error("parseSuggestions failed", e);
            return new ArrayList<>();
        }
    }

    // -------- SUMMARY EXTRACTOR -------
    private String extractSummary(String aiResponse) {
        try {
            JsonNode root = safeJsonParse(aiResponse);
            if (root == null) return "No AI summary";

            return root.has("summary")
                    ? root.get("summary").asText()
                    : "No AI summary";

        } catch (Exception e) {
            return "No AI summary";
        }
    }
}
