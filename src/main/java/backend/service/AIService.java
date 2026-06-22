package backend.service;

import backend.dto.request.ChatRequest;
import backend.dto.response.AdaptivePathResponse;
import backend.dto.response.ChatResponse;
import backend.entity.AIChatHistory;
import backend.entity.QuizAttempt;
import backend.repository.AIChatHistoryRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.QuizAttemptRepository;
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

    private static final String SYSTEM_CONTEXT =
            "Bạn là trợ lý AI của nền tảng PrepAce — hệ thống luyện thi THPT Quốc gia Việt Nam. "
          + "Hãy trả lời bằng tiếng Việt, ngắn gọn, chính xác và phù hợp với học sinh cấp 3. "
          + "Chỉ hỗ trợ các vấn đề liên quan đến học tập, ôn thi và tư vấn hướng nghiệp đại học. "
          + "Không trả lời các câu hỏi ngoài phạm vi giáo dục.";

    // ─── UC-26: AI Chatbot ───────────────────────────────────────────────────────

    @Transactional
    public ChatResponse chat(Integer studentId, ChatRequest request) {
        String contextualPrompt = request.getSubject() != null
                ? "[Môn: " + request.getSubject() + "] " + request.getMessage()
                : request.getMessage();

        String aiResponse = geminiService.ask(SYSTEM_CONTEXT, contextualPrompt);

        AIChatHistory record = new AIChatHistory();
        record.setStudentId(studentId);
        record.setQuestion(request.getMessage());
        record.setAiResponse(aiResponse);
        record.setCreatedAt(new Date());
        record.setRequestType("CHAT");
        AIChatHistory saved = chatHistoryRepository.save(record);

        if (aiResponse == null) {
            aiResponse = "AI không trả về nội dung.";
        }

        log.info("AI Chat — studentId={}, chars={}", studentId, aiResponse.length());

        return ChatResponse.builder()
                .chatId(saved.getChatId())
                .question(request.getMessage())
                .aiResponse(aiResponse)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public Page<ChatResponse> getChatHistory(Integer studentId, int page, int size) {
        return chatHistoryRepository
                .findByStudentIdOrderByCreatedAtDesc(studentId, PageRequest.of(page, size))
                .map(h -> ChatResponse.builder()
                        .chatId(h.getChatId())
                        .question(h.getQuestion())
                        .aiResponse(h.getAiResponse())
                        .createdAt(h.getCreatedAt())
                        .build());
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
        String aiPath = geminiService.ask(SYSTEM_CONTEXT, prompt);

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

        String forecast = geminiService.ask(SYSTEM_CONTEXT, prompt);
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
    public ChatResponse adviseUniversity(Integer studentId, String targetScore, String preferredMajor) {
        Double avgScore = quizAttemptRepository.findAverageScoreByStudentId(studentId).orElse(0.0);

        String prompt = String.format(
                "Học sinh điểm trung bình %s/10. Điểm mục tiêu: %s. Ngành yêu thích: %s. "
              + "Hãy tư vấn các trường đại học phù hợp tại Việt Nam, điểm chuẩn tham khảo, "
              + "và các bước chuẩn bị hồ sơ xét tuyển.",
                String.format("%.1f", avgScore),
                targetScore != null ? targetScore : "chưa xác định",
                preferredMajor != null ? preferredMajor : "chưa xác định"
        );

        String advice = geminiService.ask(SYSTEM_CONTEXT, prompt);
        saveAIHistory(studentId, "Tư vấn chọn trường ĐH - " + preferredMajor, advice, "UNIVERSITY_ADVISE");

        log.info("University Advising — studentId={}, major={}", studentId, preferredMajor);

        return ChatResponse.builder()
                .question("Tư vấn chọn trường đại học ngành: " + preferredMajor)
                .aiResponse(advice)
                .createdAt(new Date())
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
}
