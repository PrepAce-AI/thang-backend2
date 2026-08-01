package backend.service;

import backend.dto.request.ChatRequest;
import backend.dto.response.AdaptivePathViewResponse;
import backend.dto.response.ChatResponse;
import backend.dto.response.GapDiagnosisResponse;
import backend.dto.response.ScoreForecastResponse;
import backend.dto.response.UniversityAdvisingResponse;
import backend.dto.response.UniversitySuggestion;
import backend.dto.response.UniversitySuggestionView;
import backend.entity.AIChatHistory;
import backend.entity.PracticeAnswer;
import backend.entity.Question;
import backend.entity.QuizAttempt;
import backend.exceptions.GeminiException;
import backend.repository.AIChatHistoryRepository;
import backend.repository.EnrollmentRepository;
import backend.repository.PracticeAnswerRepository;
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
    private final PracticeAnswerRepository practiceAnswerRepository;
    private final ObjectMapper objectMapper;
    
    // --- CACHE ĐƠN GIẢN CHO AI ---
    // Key format: "studentId_type" (VD: "1_forecastScore")
    private final Map<String, Object> aiCache = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Long> aiCacheExpiry = new java.util.concurrent.ConcurrentHashMap<>();
    private final long CACHE_DURATION_MS = 60 * 60 * 1000; // 1 giờ
    
    public void clearCache(Integer studentId) {
        String prefix = studentId + "_";
        aiCache.keySet().removeIf(k -> k.startsWith(prefix));
        aiCacheExpiry.keySet().removeIf(k -> k.startsWith(prefix));
    }
    // -----------------------------

    private static final String SYSTEM_CONTEXT = """
        Bạn là PrepAce AI, trợ lý học tập chính thức của nền tảng PrepAce dành cho học sinh THPT Việt Nam.
        
        =========================
        VAI TRÒ
        =========================
        
        Bạn là giáo viên THPT có kinh nghiệm giảng dạy.
        
        Nhiệm vụ:
        
        - Giải thích kiến thức THPT.
        - Giải bài tập.
        - Hướng dẫn học tập.
        - Phân tích đề thi.
        - Tóm tắt lý thuyết.
        - Tư vấn phương pháp học.
        - Tư vấn chọn ngành.
        - Tư vấn chọn trường.
        
        Chỉ hỗ trợ các chủ đề liên quan đến giáo dục.
        
        Nếu người dùng hỏi về:
        
        - hack
        - crack
        - lừa đảo
        - chế tạo vũ khí
        - nội dung người lớn
        - bạo lực
        - chính trị cực đoan
        
        hãy từ chối lịch sự và chuyển hướng sang chủ đề học tập.
        
        =========================
        NGÔN NGỮ
        =========================
        
        Luôn trả lời bằng tiếng Việt.
        
        Giọng văn:
        
        - thân thiện
        - gần gũi
        - dễ hiểu
        - giống giáo viên đang giảng
        
        Không tự giới thiệu.
        
        Không mở đầu bằng:
        
        "Xin chào"
        
        "Chào em"
        
        "Hôm nay chúng ta sẽ..."
        
        Trả lời trực tiếp vào nội dung.
        
            =========================
                                                 FORMAT
                                                 =========================
            
                                                 Luôn trả lời bằng Markdown.
            
                                                 Được phép sử dụng:
            
                                                 # Tiêu đề
            
                                                 ## Tiêu đề nhỏ
            
                                                 - Bullet List
            
                                                 1. Number List
            
                                                 **In đậm**
            
                                                 *In nghiêng*
            
                                                 Bảng Markdown nếu cần.
            
                                                 Nếu có công thức toán:
            
                                                 Inline:
            
                                                 $f(x)=x^2$
            
                                                 Block:
            
                                                 $$
                                                 f'(x)=\\lim_{h\\to0}\\frac{f(x+h)-f(x)}{h}
                                                 $$
            
                                                 Không sử dụng HTML.
            
                                                 Không trả JSON.
            
                                                 Không sử dụng ```markdown.
        
        =========================
        CHẤT LƯỢNG
        =========================
        
        Nếu người dùng chỉ nhập tên một chủ đề.
        
        Ví dụ:
        
        Đạo hàm
        
        Nguyên hàm
        
        Dao động điều hòa
        
        Điện xoay chiều
        
        Este
        
        ...
        
        => hiểu rằng người dùng muốn học toàn bộ chủ đề.
        
        Không chỉ trả lời định nghĩa.
        
        Không bỏ sót các ý quan trọng.
        
        Giải thích chính xác, dễ hiểu và có ví dụ.
        """;

    private static final String CHAT_CONTEXT = """
        Đây là cuộc trò chuyện thông thường.
        
        Nếu người dùng hỏi kiến thức học tập.
        
        => giải thích vừa đủ.
        
        Nếu người dùng chỉ nhập tên một chủ đề.
        
        Ví dụ:
        
        Đạo hàm
        
        Nguyên hàm
        
        Dao động điều hòa
        
        => tự động chuyển sang chế độ GIẢNG BÀI TOÀN DIỆN.
        
        Nếu người dùng hỏi:
        
        "Tóm tắt..."
        
        => chuyển sang chế độ tóm tắt.
        
        Nếu người dùng hỏi:
        
        "Giải..."
        
        => chuyển sang chế độ giải bài.
        
        Nếu người dùng hỏi tư vấn học.
        
        Hãy trả lời như một giáo viên.
        
        Không lan man.
        
        Không nói chuyện như chatbot.
        """;

    private static final String SUMMARY_CONTEXT = """
        Đây là yêu cầu tóm tắt.
        
        Không giảng dài.
        
        Hãy trình bày:
        
        # Tên chủ đề
        
        ## Khái niệm
        
        ## Công thức
        
        ## Ý chính
        
        ## Điều cần nhớ
        
        ## Mẹo học nhanh
        
        ## Những lỗi thường gặp
        
        Độ dài khoảng 300-600 từ.
        
        Ưu tiên ngắn gọn, dễ ôn tập.
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
    /**
     * Context dùng cho các đoạn nhận xét ngắn (Gap Diagnosis / Adaptive Path / Score Forecast).
     * Khác SYSTEM_CONTEXT/ADAPTIVE_CONTEXT (bắt buộc JSON) — ở đây chỉ cần văn xuôi thuần
     * để hiển thị thẳng cho học sinh, không phải parse JSON.
     */
    private static final String INSIGHT_SUMMARY_CONTEXT = """
        Bạn là AI Học tập của PrepAce. Học sinh gửi cho bạn dữ liệu chấm điểm THẬT từ lịch sử
        làm bài (không phải dữ liệu bịa). Hãy viết một đoạn nhận xét 3-5 câu, tiếng Việt,
        giọng văn khích lệ nhưng thẳng thắn và cụ thể — chỉ ra học sinh mạnh/yếu ở đâu dựa
        đúng trên số liệu được cung cấp, và nên ưu tiên làm gì tiếp theo.
        TUYỆT ĐỐI KHÔNG dùng markdown, KHÔNG dùng JSON, KHÔNG liệt kê gạch đầu dòng —
        chỉ viết văn xuôi liền mạch, không mở đầu kiểu "Dựa trên dữ liệu bạn cung cấp...".
        """;

    private static final String UNIVERSITY_SUMMARY_CONTEXT = """
        Bạn là chuyên gia tư vấn tuyển sinh đại học Việt Nam. Học sinh gửi điểm dự đoán khối
        thi và danh sách trường/ngành đã được hệ thống tính toán sẵn (không cần bạn đề xuất
        trường mới). Hãy viết một đoạn nhận xét ngắn 3-4 câu bằng tiếng Việt về khả năng đỗ
        tổng quan và lời khuyên ôn tập để tăng cơ hội. TUYỆT ĐỐI KHÔNG markdown, KHÔNG JSON,
        KHÔNG gạch đầu dòng, chỉ văn xuôi liền mạch.
        """;

    private static final String FULL_LESSON_CONTEXT = """
        Đây là yêu cầu GIẢNG BÀI.
        
        Hãy trình bày theo đúng thứ tự sau.
        
        # Tên chủ đề
        
        ## 1. Định nghĩa
        
        ## 2. Bản chất
        
        ## 3. Kiến thức nền cần nhớ
        
        ## 4. Công thức (nếu có)
        
        Giải thích từng công thức.
        
        Nêu ý nghĩa từng ký hiệu.
        
        ## 5. Phân tích trực quan
        
        Giải thích bằng ngôn ngữ dễ hiểu.
        
        Có ví dụ thực tế.
        
        ## 6. Ví dụ minh họa
        
        Ít nhất 2 ví dụ.
        
        Giải thích từng bước.
        
        ## 7. Mẹo ghi nhớ
        
        ## 8. Các lỗi học sinh thường gặp
        
        ## 9. Cách áp dụng
        
        ## 10. Tổng kết
        
        Độ dài khoảng 1000-1800 từ.
        
        Không viết quá ngắn.
        
        Không bỏ qua các mục.
        """;

    private static final String SOLUTION_CONTEXT = """
        Đây là yêu cầu giải bài tập.
        
        Không chỉ đưa đáp án.
        
        Luôn trình bày:
        
        ## Phân tích đề
        
        ## Kiến thức cần dùng
        
        ## Các bước giải
        
        Bước 1
        
        Bước 2
        
        Bước 3
        
        ...
        
        ## Đáp án
        
        ## Giải thích vì sao
        
        Nếu có nhiều cách giải.
        
        Hãy nêu cách nhanh nhất.
        
        Nếu bài toán dễ gây nhầm.
        
        Hãy chỉ rõ lỗi thường gặp.
        """;

    private boolean isSummary(String question) {

        question = question.toLowerCase();

        return question.contains("tóm tắt")
                || question.contains("tóm lược")
                || question.contains("ghi chú")
                || question.contains("mindmap")
                || question.contains("sơ đồ tư duy");
    }

    private boolean isExercise(String question) {

        question = question.toLowerCase();

        return question.contains("giải")
                || question.contains("tính")
                || question.contains("chứng minh")
                || question.contains("làm giúp")
                || question.contains("bài tập")
                || question.contains("câu ")
                || question.contains("đề");
    }

    private boolean isConcept(String q){

        q = q.trim().toLowerCase();

        if(q.split("\\s+").length <= 4){
            return true;
        }

        return q.contains("giải thích")
                || q.contains("khái niệm")
                || q.contains("định nghĩa")
                || q.contains("lý thuyết")
                || q.contains("học")
                || q.contains("tìm hiểu");
    }
    // ─── UC-26: AI Chatbot ───────────────────────────────────────────────────────

    @Transactional
    public ChatResponse chat(Integer studentId, ChatRequest request) {
        String contextualPrompt = request.getSubject() != null
                ? "[Môn: " + request.getSubject() + "] " + request.getMessage()
                : request.getMessage();
        String aiResponse;
        try {
            String question = request.getMessage().toLowerCase();
            String taskPrompt;

            if (isSummary(question)) {
                taskPrompt = contextualPrompt + "\n\n" + SUMMARY_CONTEXT;
            }
            else if (isExercise(question)) {
                taskPrompt = contextualPrompt + "\n\n" + SOLUTION_CONTEXT;
            }
            else if (isConcept(question)) {
                taskPrompt = contextualPrompt + "\n\n" + FULL_LESSON_CONTEXT;
            }
            else {
                taskPrompt = contextualPrompt + "\n\n" + CHAT_CONTEXT;

            }
            aiResponse = geminiService.ask(
                    SYSTEM_CONTEXT,
                    taskPrompt
            );
            log.info("========== RAW ==========");
            log.info(aiResponse);
            log.info("=========================");

            if (aiResponse == null || aiResponse.isBlank()) {
                aiResponse = "Xin lỗi, AI hiện không phản hồi.";
            }
// Chỉ để tương thích với prompt cũ
            if (aiResponse.trim().startsWith("{")) {
                JsonNode json = safeJsonParse(aiResponse);
                if (json != null) {
                    if (json.has("answer"))
                        aiResponse = json.get("answer").asText();

                    else if (json.has("response"))
                        aiResponse = json.get("response").asText();

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

    /**
     * Chẩn đoán lỗ hổng THEO TỪNG CHỦ ĐỀ (topic), tính trực tiếp từ các câu học sinh
     * đã làm sai/đúng thật (bảng PracticeAnswers + Questions.topic) — thay cho bản cũ
     * nhóm "cognitive level" giả bằng quizId % 4 (không phản ánh đúng năng lực thật).
     */
    @Transactional
    public GapDiagnosisResponse diagnoseGaps(Integer studentId) {
        String cacheKey = studentId + "_gapDiagnosis";
        if (aiCache.containsKey(cacheKey) && aiCacheExpiry.get(cacheKey) > System.currentTimeMillis()) {
            return (GapDiagnosisResponse) aiCache.get(cacheKey);
        }

        List<TopicStat> topicStats = computeTopicStats(studentId);
        int totalAnswered = topicStats.stream().mapToInt(TopicStat::total).sum();

        if (totalAnswered == 0) {
            return GapDiagnosisResponse.builder()
                    .hasData(false)
                    .overallAccuracy(0)
                    .summary("Bạn chưa làm bài kiểm tra/luyện đề nào. Hãy làm Kiểm tra đầu vào để AI phân tích năng lực của bạn.")
                    .gaps(List.of())
                    .build();
        }

        int totalWrong = topicStats.stream().mapToInt(TopicStat::wrong).sum();
        int overallAccuracy = accuracyPercent(totalAnswered, totalWrong);

        // Lỗ hổng = chủ đề có ít nhất 1 câu sai, ưu tiên chủ đề yếu nhất trước
        List<TopicStat> weak = topicStats.stream()
                .filter(t -> t.wrong() > 0)
                .sorted(Comparator.comparingInt((TopicStat t) -> accuracyPercent(t.total(), t.wrong()))
                        .thenComparing(Comparator.comparingInt(TopicStat::wrong).reversed()))
                .toList();

        List<GapDiagnosisResponse.GapView> gaps = weak.stream()
                .map(t -> {
                    int acc = accuracyPercent(t.total(), t.wrong());
                    String severity = acc < 40 ? "Hổng nặng" : acc < 70 ? "Chưa vững" : "Cần tinh chỉnh";
                    String color = acc < 40 ? "#ef4444" : acc < 70 ? "#f59e0b" : "#3b82f6";
                    String rec = String.format(
                            "Phần «%s» (môn %s) sai %d/%d câu, đúng %d%% — hãy xem lại lý thuyết, "
                                    + "đọc kỹ lời giải các câu sai trong lịch sử làm bài rồi luyện lại đề %s để củng cố.",
                            t.topic(), t.subject(), t.wrong(), t.total(), acc, t.subject());
                    return GapDiagnosisResponse.GapView.builder()
                            .subject(t.subject())
                            .topic(t.topic())
                            .severity(severity)
                            .color(color)
                            .accuracy(acc)
                            .recommendation(rec)
                            .build();
                })
                .toList();

        String ruleSummary = buildInsightSummary(overallAccuracy, weak.size());
        String prompt = buildGapPrompt(overallAccuracy, weak);
        String aiSummary = askGeminiWithFallback(INSIGHT_SUMMARY_CONTEXT, prompt, ruleSummary);

        saveAIHistory(studentId, "Phân tích lỗ hổng kiến thức", aiSummary, "GAP_DIAGNOSIS");
        log.info("Gap Diagnosis — studentId={}, overallAccuracy={}%, weakTopics={}", studentId, overallAccuracy, weak.size());

        GapDiagnosisResponse res = GapDiagnosisResponse.builder()
                .hasData(true)
                .overallAccuracy(overallAccuracy)
                .summary(aiSummary)
                .gaps(gaps)
                .build();
                
        aiCache.put(cacheKey, res);
        aiCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_DURATION_MS);
        
        return res;
    }

    // ─── UC-27: Adaptive Path Generation ────────────────────────────────────────

    /** Biểu đồ năng lực theo môn + lộ trình hành động cụ thể, tính từ dữ liệu bài làm thật. */
    @Transactional
    public AdaptivePathViewResponse generateAdaptivePath(Integer studentId) {
        List<TopicStat> topicStats = computeTopicStats(studentId);
        int totalAnswered = topicStats.stream().mapToInt(TopicStat::total).sum();

        if (totalAnswered == 0) {
            return AdaptivePathViewResponse.builder()
                    .hasData(false)
                    .skills(List.of())
                    .path(List.of())
                    .aiSummary("Bạn chưa có dữ liệu học tập. Hãy làm Kiểm tra đầu vào hoặc Luyện đề để AI xây dựng lộ trình cá nhân hóa.")
                    .build();
        }

        Map<String, int[]> bySubject = aggregateBySubject(topicStats);

        List<AdaptivePathViewResponse.SkillView> skills = bySubject.entrySet().stream()
                .map(e -> {
                    int acc = accuracyPercent(e.getValue()[0], e.getValue()[1]);
                    String color = acc >= 70 ? "#10b981" : acc >= 50 ? "#f59e0b" : "#ef4444";
                    String status = acc >= 80 ? "Tốt" : acc >= 65 ? "Khá" : acc >= 50 ? "Trung bình" : "Cần cải thiện";
                    return AdaptivePathViewResponse.SkillView.builder()
                            .subject(e.getKey())
                            .score(acc)
                            .color(color)
                            .status(status)
                            .warning(acc < 50)
                            .build();
                })
                .sorted(Comparator.comparing(AdaptivePathViewResponse.SkillView::getScore))
                .toList();

        List<TopicStat> weak = topicStats.stream()
                .filter(t -> t.wrong() > 0)
                .sorted(Comparator.comparingInt((TopicStat t) -> accuracyPercent(t.total(), t.wrong())))
                .toList();
        List<TopicStat> strong = topicStats.stream()
                .filter(t -> t.wrong() == 0 && t.total() >= 2)
                .toList();

        List<AdaptivePathViewResponse.PathStepView> path = new ArrayList<>();
        
        // Cảnh báo khẩn cấp cho các môn ở ngưỡng tử (< 40%)
        for (AdaptivePathViewResponse.SkillView skill : skills) {
            if (skill.getScore() < 40) {
                String icon = "🚨";
                String action = "Vào khóa học để cấp cứu môn này";
                if (skill.getSubject().toLowerCase().contains("anh")) action = "Vào khóa học lấy lại gốc Tiếng Anh";
                else if (skill.getSubject().toLowerCase().contains("lý") || skill.getSubject().toLowerCase().contains("hóa")) action = "Vào khóa học ôn khẩn cấp lý thuyết";
                else if (skill.getSubject().toLowerCase().contains("toán")) action = "Vào khóa học cày lại chuyên đề Toán";
                
                path.add(AdaptivePathViewResponse.PathStepView.builder()
                        .type("video").icon(icon)
                        .title("BÁO ĐỘNG ĐỎ: Môn " + skill.getSubject())
                        .subject(skill.getSubject())
                        .reason("Môn học này đang ở ngưỡng báo động (chỉ đạt " + skill.getScore() + "%). Cần học lại từ cơ bản ngay lập tức!")
                        .action(action)
                        .build());
            }
        }
        
        if (!weak.isEmpty()) {
            String subjectsStr = weak.stream()
                    .map(TopicStat::subject)
                    .distinct()
                    .collect(Collectors.joining(", "));

            int totalWrong = weak.stream().mapToInt(TopicStat::wrong).sum();
            int totalQuestions = weak.stream().mapToInt(TopicStat::total).sum();

            path.add(AdaptivePathViewResponse.PathStepView.builder()
                    .type("practice").icon("🔄")
                    .title("Ôn tập bù lỗ hổng")
                    .subject("")
                    .reason("AI phát hiện bạn bị hổng kiến thức ở các môn: " + subjectsStr + " (tổng cộng sai " + totalWrong + "/" + totalQuestions + " câu).")
                    .action("Vào luyện đề ngay")
                    .build());
        }
        if (!strong.isEmpty()) {
            TopicStat s1 = strong.get(0);
            path.add(AdaptivePathViewResponse.PathStepView.builder()
                    .type("next").icon("📖")
                    .title("Thử sức đề khó hơn: " + s1.topic())
                    .subject(s1.subject())
                    .reason("Bạn đã làm đúng 100% các câu " + s1.topic() + " (" + s1.total() + " câu) — sẵn sàng thử đề Thi thử để nâng cao.")
                    .action("Vào luyện đề")
                    .build());
        }
        if (path.isEmpty()) {
            path.add(AdaptivePathViewResponse.PathStepView.builder()
                    .type("next").icon("🎯")
                    .title("Tiếp tục luyện đề để AI có thêm dữ liệu")
                    .subject("")
                    .reason("Bạn đã làm đúng gần như toàn bộ câu hỏi đã thử — hãy thử đề khó hơn hoặc môn mới để AI đánh giá chính xác hơn.")
                    .action("Luyện đề mới")
                    .build());
        }

        int overallAccuracy = (int) Math.round(bySubject.values().stream()
                .mapToInt(v -> accuracyPercent(v[0], v[1]))
                .average().orElse(0));
        String ruleSummary = buildInsightSummary(overallAccuracy, weak.size());
        String prompt = buildAdaptivePrompt(skills, weak);
        String aiSummary = askGeminiWithFallback(INSIGHT_SUMMARY_CONTEXT, prompt, ruleSummary);

        saveAIHistory(studentId, "Tạo lộ trình học thích ứng", aiSummary, "ADAPTIVE_PATH");
        log.info("Adaptive Path — studentId={}, subjects={}, weakTopics={}", studentId, skills.size(), weak.size());

        return AdaptivePathViewResponse.builder()
                .hasData(true)
                .skills(skills)
                .path(path)
                .aiSummary(aiSummary)
                .build();
    }

    // ─── UC-29: AI Score Forecasting ────────────────────────────────────────────

    /** Dự đoán điểm theo TỪNG MÔN dựa trên độ chính xác thật + xu hướng các lượt gần đây. */
    @Transactional
    public ScoreForecastResponse forecastScore(Integer studentId) {
        String cacheKey = studentId + "_forecastScore";
        if (aiCache.containsKey(cacheKey) && aiCacheExpiry.get(cacheKey) > System.currentTimeMillis()) {
            return (ScoreForecastResponse) aiCache.get(cacheKey);
        }

        List<TopicStat> topicStats = computeTopicStats(studentId);
        int totalAnswered = topicStats.stream().mapToInt(TopicStat::total).sum();

        List<QuizAttempt> submitted = quizAttemptRepository.findByStudentIdOrderBySubmittedAtDesc(studentId)
                .stream().filter(a -> a.getSubmittedAt() != null).toList();

        if (totalAnswered == 0 || submitted.isEmpty()) {
            return ScoreForecastResponse.builder()
                    .hasData(false)
                    .summary("Bạn chưa có lịch sử làm bài nào để dự đoán điểm. Hãy luyện đề để AI có dữ liệu phân tích.")
                    .subjects(List.of())
                    .build();
        }

        Map<String, int[]> bySubject = aggregateBySubject(topicStats);
        double trend = calculateTrend(submitted.stream().limit(5).toList());
        double trendAdjust = Math.max(-1.0, Math.min(1.0, trend * 0.3));

        List<ScoreForecastResponse.SubjectForecast> subjects = bySubject.entrySet().stream()
                .map(e -> {
                    double currentAvg = Math.round(accuracyPercent(e.getValue()[0], e.getValue()[1]) / 10.0 * 10) / 10.0;
                    double predicted = Math.max(0, Math.min(10, Math.round((currentAvg + trendAdjust) * 10) / 10.0));
                    String color = predicted >= 8 ? "#10b981" : predicted >= 6.5 ? "#3b82f6" : predicted >= 5 ? "#f59e0b" : "#ef4444";
                    return ScoreForecastResponse.SubjectForecast.builder()
                            .subject(e.getKey())
                            .currentAvg(currentAvg)
                            .predictedScore(predicted)
                            .color(color)
                            .build();
                })
                .sorted(Comparator.comparing(ScoreForecastResponse.SubjectForecast::getSubject))
                .toList();

        double currentTotal = Math.round(subjects.stream()
                .mapToDouble(ScoreForecastResponse.SubjectForecast::getCurrentAvg).average().orElse(0) * 10) / 10.0;
        double predictedTotal = Math.round(subjects.stream()
                .mapToDouble(ScoreForecastResponse.SubjectForecast::getPredictedScore).average().orElse(0) * 10) / 10.0;

        int confidence = Math.min(95, 40 + submitted.size() * 8);
        String trendLabel = trend > 0.3 ? "Tăng +" + String.format("%.1f", trend) + " điểm"
                : trend < -0.3 ? "Giảm " + String.format("%.1f", trend) + " điểm"
                : "Ổn định";

        String ruleSummary = buildRuleBasedForecast(currentTotal, trend);
        String prompt = String.format(
                "Điểm trung bình hiện tại %.1f/10, điểm dự đoán %.1f/10, xu hướng: %s (dựa trên %d lượt thi gần nhất). "
                        + "Hãy viết nhận xét ngắn gọn kèm lời khuyên cụ thể để cải thiện.",
                currentTotal, predictedTotal, trendLabel, submitted.size());
        String summary = askGeminiWithFallback(INSIGHT_SUMMARY_CONTEXT, prompt, ruleSummary);

        saveAIHistory(studentId, "Dự đoán điểm thi THPT QG", summary, "SCORE_FORECAST");
        log.info("Score Forecast — studentId={}, current={}, predicted={}", studentId, currentTotal, predictedTotal);

        ScoreForecastResponse res = ScoreForecastResponse.builder()
                .hasData(true)
                .currentTotal(currentTotal)
                .predictedTotal(predictedTotal)
                .confidence(confidence)
                .trend(trendLabel)
                .summary(summary)
                .subjects(subjects)
                .build();
                
        aiCache.put(cacheKey, res);
        aiCacheExpiry.put(cacheKey, System.currentTimeMillis() + CACHE_DURATION_MS);
        
        return res;
    }

    // ─── UC-30: AI University Advising ──────────────────────────────────────────

    /** Khối thi → 3 môn xét tuyển. Môn nào hệ thống chưa có kho câu hỏi (Sinh/Văn/Sử/Địa)
     *  sẽ được ước tính bằng điểm trung bình chung — nêu rõ trong summary để không gây hiểu nhầm. */
    private static final Map<String, List<String>> BLOCK_SUBJECTS = Map.of(
            "A00", List.of("Toán", "Vật Lý", "Hóa Học"),
            "A01", List.of("Toán", "Vật Lý", "Tiếng Anh"),
            "B00", List.of("Toán", "Hóa Học"),
            "C00", List.of(),
            "D01", List.of("Toán", "Tiếng Anh")
    );

    private record UniPick(String university, String major, double benchmark) {}

    /** Danh sách trường/ngành thật (không bịa) theo khối, kèm điểm chuẩn tham khảo gần nhất */
    private static final Map<String, List<UniPick>> BLOCK_UNIVERSITIES = Map.of(
            "A00", List.of(
                    new UniPick("Đại học Bách Khoa Hà Nội", "Kỹ thuật Cơ khí", 24.5),
                    new UniPick("Đại học Bách Khoa TP.HCM", "Kỹ thuật Điện - Điện tử", 25.0),
                    new UniPick("Đại học Xây dựng Hà Nội", "Kỹ thuật Xây dựng", 20.0),
                    new UniPick("Đại học Công nghiệp TP.HCM", "Công nghệ Kỹ thuật Ô tô", 18.5),
                    new UniPick("Đại học Giao thông Vận tải", "Kỹ thuật Giao thông", 19.5)
            ),
            "A01", List.of(
                    new UniPick("Đại học Bách Khoa Hà Nội", "Khoa học Máy tính", 27.0),
                    new UniPick("Đại học Ngoại thương", "Kinh doanh Quốc tế", 27.5),
                    new UniPick("Đại học Kinh tế Quốc dân", "Logistics & Quản lý chuỗi cung ứng", 26.5),
                    new UniPick("Học viện Công nghệ Bưu chính Viễn thông", "Công nghệ thông tin", 24.0),
                    new UniPick("Đại học FPT", "Kỹ thuật phần mềm", 21.0)
            ),
            "B00", List.of(
                    new UniPick("Đại học Y Hà Nội", "Y khoa", 28.5),
                    new UniPick("Đại học Y Dược TP.HCM", "Y khoa", 27.8),
                    new UniPick("Đại học Dược Hà Nội", "Dược học", 26.0),
                    new UniPick("Đại học Y tế Công cộng", "Y tế Công cộng", 20.0),
                    new UniPick("Học viện Nông nghiệp Việt Nam", "Công nghệ Sinh học", 18.0)
            ),
            "C00", List.of(
                    new UniPick("Đại học Khoa học Xã hội và Nhân văn (ĐHQGHN)", "Báo chí", 25.5),
                    new UniPick("Đại học Sư phạm Hà Nội", "Sư phạm Ngữ văn", 24.0),
                    new UniPick("Đại học Luật Hà Nội", "Luật", 25.0),
                    new UniPick("Học viện Báo chí và Tuyên truyền", "Truyền thông Đa phương tiện", 24.5),
                    new UniPick("Đại học Văn hóa Hà Nội", "Quản lý Văn hóa", 18.0)
            ),
            "D01", List.of(
                    new UniPick("Đại học Ngoại thương", "Kinh tế Đối ngoại", 27.8),
                    new UniPick("Đại học Hà Nội (HANU)", "Ngôn ngữ Anh", 24.5),
                    new UniPick("Đại học Kinh tế Quốc dân", "Marketing", 26.8),
                    new UniPick("Học viện Ngoại giao", "Quan hệ Quốc tế", 25.5),
                    new UniPick("Đại học RMIT Việt Nam", "Quản trị Kinh doanh", 20.0)
            )
    );

    /**
     * Tư vấn ngành/trường theo khối, dựa trên điểm dự đoán TỪNG MÔN tính từ dữ liệu bài làm thật.
     * Danh sách trường/ngành được tính RULE-BASED (không phụ thuộc Gemini) để luôn hoạt động
     * ổn định kể cả khi hết quota; Gemini chỉ được dùng để viết thêm 1 đoạn nhận xét ngắn.
     */
    @Transactional
    public UniversityAdvisingResponse getUniversityAdvising(Integer studentId, String block) {
        String normalizedBlock = BLOCK_SUBJECTS.containsKey(block) ? block : "A00";
        String cacheKey = studentId + "_universityAdvising_" + normalizedBlock;
        if (aiCache.containsKey(cacheKey) && aiCacheExpiry.get(cacheKey) > System.currentTimeMillis()) {
            return (UniversityAdvisingResponse) aiCache.get(cacheKey);
        }

        List<TopicStat> topicStats = computeTopicStats(studentId);
        int totalAnswered = topicStats.stream().mapToInt(TopicStat::total).sum();

        if (totalAnswered == 0) {
            return UniversityAdvisingResponse.builder()
                    .hasData(false)
                    .block(normalizedBlock)
                    .predictedScore(0.0)
                    .summary("Bạn chưa có dữ liệu làm bài. Hãy làm Kiểm tra đầu vào để AI dự đoán điểm khối và tư vấn trường phù hợp.")
                    .suggestions(List.of())
                    .build();
        }

        Map<String, int[]> bySubject = aggregateBySubject(topicStats);
        int totalWrong = topicStats.stream().mapToInt(TopicStat::wrong).sum();
        double overallScore10 = accuracyPercent(totalAnswered, totalWrong) / 10.0;

        List<String> subjectsForBlock = BLOCK_SUBJECTS.get(normalizedBlock);
        double predictedScore = 0;
        boolean missingSubject = false;
        for (String subj : subjectsForBlock) {
            int[] c = bySubject.get(subj);
            if (c != null && c[0] > 0) {
                predictedScore += accuracyPercent(c[0], c[1]) / 10.0;
            } else {
                predictedScore += overallScore10; // chưa có dữ liệu môn này -> ước tính bằng điểm TB chung
                missingSubject = true;
            }
        }
        int stillNeeded = 3 - subjectsForBlock.size(); // C00 thiếu cả 3 môn, B00/D01 thiếu 1 môn
        for (int i = 0; i < stillNeeded; i++) {
            predictedScore += overallScore10;
            missingSubject = true;
        }
        predictedScore = Math.round(predictedScore * 10) / 10.0;
        final double finalPredictedScore = predictedScore;

        // Xếp hạng trường theo % khả năng đỗ = chênh lệch điểm dự đoán so với điểm chuẩn tham khảo
        List<UniPick> picks = BLOCK_UNIVERSITIES.getOrDefault(normalizedBlock, BLOCK_UNIVERSITIES.get("A00"));
        List<UniversitySuggestionView> suggestions = picks.stream()
                .map(p -> {
                    int chance = (int) Math.max(5, Math.min(97, Math.round(50 + (finalPredictedScore - p.benchmark()) * 8)));
                    String chanceLabel = chance >= 70 ? "Khả năng cao" : chance >= 40 ? "Cân nhắc kỹ" : "Rủi ro cao, cần nỗ lực thêm";
                    String color = chance >= 70 ? "#10b981" : chance >= 40 ? "#f59e0b" : "#ef4444";
                    String note = String.format(
                            "Điểm chuẩn tham khảo gần nhất khoảng %.1f, điểm dự đoán của bạn là %.1f — %s.",
                            p.benchmark(), finalPredictedScore,
                            chance >= 70 ? "cơ hội trúng tuyển khá tốt nếu giữ phong độ"
                                    : chance >= 40 ? "cần cải thiện thêm để chắc chắn đỗ"
                                    : "khoảng cách còn khá xa, nên cân nhắc thêm nguyện vọng dự phòng");
                    return UniversitySuggestionView.builder()
                            .university(p.university())
                            .major(p.major())
                            .chancePercent(chance)
                            .benchmark(String.format("%.1f", p.benchmark()))
                            .chanceLabel(chanceLabel)
                            .color(color)
                            .note(note)
                            .build();
                })
                .sorted(Comparator.comparing(UniversitySuggestionView::getChancePercent).reversed())
                .toList();

        String ruleSummary = buildUniversitySummary(normalizedBlock, predictedScore, missingSubject, suggestions);
        String prompt = String.format(
                "Điểm dự đoán khối %s của học sinh là %.1f/30. Top trường phù hợp (đã tính sẵn): %s. "
                        + "Hãy viết nhận xét ngắn về khả năng đỗ tổng quan và lời khuyên để tăng cơ hội.",
                normalizedBlock, predictedScore,
                suggestions.stream().limit(3)
                        .map(s -> s.getUniversity() + " (" + s.getChancePercent() + "%)")
                        .collect(Collectors.joining(", ")));
        String summary = askGeminiWithFallback(UNIVERSITY_SUMMARY_CONTEXT, prompt, ruleSummary);

        saveAIHistory(studentId, "Tư vấn chọn ngành & trường (khối " + normalizedBlock + ")", summary, "UNIVERSITY_ADVISE");
        log.info("University Advising — studentId={}, block={}, predictedScore={}", studentId, normalizedBlock, predictedScore);

        UniversityAdvisingResponse res = UniversityAdvisingResponse.builder()
                .hasData(true)
                .block(normalizedBlock)
                .predictedScore(predictedScore)
                .summary(summary)
                .suggestions(suggestions)
                .build();
                
        aiCache.put(studentId + "_universityAdvising_" + normalizedBlock, res);
        aiCacheExpiry.put(studentId + "_universityAdvising_" + normalizedBlock, System.currentTimeMillis() + CACHE_DURATION_MS);
        
        return res;
    }

    // ─── Private helpers ────────────────────────────────────────────────────────

    /**
     * Gọi Gemini, tự động fallback sang rule-based nếu lỗi (429 hết quota, 401/403 key hỏng,
     * timeout...). Áp dụng cho mọi endpoint AI trừ chat() (đã có xử lý riêng chi tiết hơn),
     * để không endpoint nào lộ nguyên JSON lỗi 429 thẳng ra HTTP response cho người dùng.
     */
    private String askGeminiWithFallback(String systemContext, String prompt, String fallbackMessage) {
        try {
            return geminiService.ask(systemContext, prompt);
        } catch (GeminiException e) {
            log.warn("Gemini lỗi (status={}), dùng fallback rule-based: {}", e.getStatusCode(), e.getMessage());
            return fallbackMessage;
        } catch (Exception e) {
            log.error("Gemini lỗi không xác định, dùng fallback rule-based", e);
            return fallbackMessage;
        }
    }

    /** Dự đoán điểm rule-based khi Gemini không khả dụng — dùng điểm trung bình + xu hướng gần đây */
    private String buildRuleBasedForecast(double avgScore, double trend) {
        String trendText = trend > 0.3
                ? "đang có xu hướng TĂNG (+" + String.format("%.1f", trend) + " điểm so với các lần thi trước)"
                : trend < -0.3
                ? "đang có xu hướng GIẢM (" + String.format("%.1f", trend) + " điểm so với các lần thi trước)"
                : "khá ổn định qua các lần thi gần đây";

        double predicted = Math.max(0, Math.min(10, avgScore + trend * 0.5));
        String advice = predicted >= 8
                ? "Bạn đang ở mức Giỏi — hãy luyện thêm đề khó (Vận dụng cao) và giữ phong độ ổn định."
                : predicted >= 6.5
                ? "Bạn đang ở mức Khá — tập trung lấp các lỗ hổng kiến thức còn lại và luyện tốc độ làm bài."
                : predicted >= 5
                ? "Bạn đang ở mức Trung bình — nên ôn lại kiến thức nền tảng trước khi luyện đề khó."
                : "Bạn cần ôn tập lại từ gốc — hãy làm Kiểm tra đầu vào cho từng môn để xác định đúng lỗ hổng.";

        return String.format(
                "🚦 AI đang quá tải/hết quota nên đây là dự đoán rule-based (không phải Gemini): "
                        + "điểm trung bình hiện tại của bạn là %.1f/10, %s. "
                        + "Dự đoán điểm thi THPT QG: khoảng %.1f/10. %s",
                avgScore, trendText, predicted, advice
        );
    }

    private double calculateTrend(List<QuizAttempt> recent) {
        if (recent.size() < 2) return 0.0;
        double first = recent.get(recent.size() - 1).getScore() != null ? recent.get(recent.size() - 1).getScore() : 0;
        double last = recent.get(0).getScore() != null ? recent.get(0).getScore() : 0;
        return last - first;
    }

    // ─── Phân tích lỗ hổng thật từ PracticeAnswers (nguồn dữ liệu chung cho cả 4 tính năng AI) ──

    /** Thống kê 1 chủ đề (topic): tổng số câu đã làm và số câu sai trong chủ đề đó */
    private record TopicStat(String subject, String topic, int total, int wrong) {}

    private int accuracyPercent(int total, int wrong) {
        if (total <= 0) return 0;
        return (int) Math.round((total - wrong) * 100.0 / total);
    }

    /** Gom mọi câu đã chấm của học sinh (mọi lượt Entry Test/Luyện đề/Thi thử đã nộp) theo topic */
    private List<TopicStat> computeTopicStats(Integer studentId) {
        List<PracticeAnswer> answers = practiceAnswerRepository.findGradedByStudentId(studentId);
        Map<String, int[]> agg = new LinkedHashMap<>(); // key "subject||topic" -> [total, wrong]
        for (PracticeAnswer pa : answers) {
            Question q = pa.getQuestion();
            String quizSubject = (q.getQuiz() != null && q.getQuiz().getSubject() != null && !q.getQuiz().getSubject().isBlank()) ? q.getQuiz().getSubject() : "Khác";
            String subject = (q.getSubject() != null && !q.getSubject().isBlank()) ? q.getSubject() : quizSubject;
            String topic = (q.getTopic() != null && !q.getTopic().isBlank()) ? q.getTopic() : subject;
            topic = topic.replaceAll(" \\(Mã đề .*?\\)", "").trim(); // Xóa chuỗi " (Mã đề ...)" để gộp chuẩn xác
            int[] c = agg.computeIfAbsent(subject + "||" + topic, k -> new int[2]);
            c[0]++;
            if (!Boolean.TRUE.equals(pa.getIsCorrect())) c[1]++;
        }
        List<TopicStat> stats = new ArrayList<>();
        for (Map.Entry<String, int[]> e : agg.entrySet()) {
            String[] parts = e.getKey().split("\\|\\|", 2);
            stats.add(new TopicStat(parts[0], parts[1], e.getValue()[0], e.getValue()[1]));
        }
        return stats;
    }

    /** Cộng dồn thống kê topic lên cấp độ môn học — dùng cho radar chart / dự đoán điểm / tư vấn khối */
    private Map<String, int[]> aggregateBySubject(List<TopicStat> topicStats) {
        Map<String, int[]> bySubject = new LinkedHashMap<>();
        for (TopicStat t : topicStats) {
            int[] c = bySubject.computeIfAbsent(t.subject(), k -> new int[2]);
            c[0] += t.total();
            c[1] += t.wrong();
        }
        return bySubject;
    }

    /** Nhận xét tổng quan rule-based — dùng khi Gemini lỗi/hết quota, đồng bộ với buildGapAnalysis() phía FE */
    private String buildInsightSummary(int overallAccuracy, int weakCount) {
        if (overallAccuracy >= 80)
            return "Xuất sắc! Bạn đạt độ chính xác " + overallAccuracy + "%. Nền tảng kiến thức rất vững, chỉ cần tinh chỉnh vài điểm nhỏ để hoàn hảo.";
        if (overallAccuracy >= 65)
            return "Khá tốt! Độ chính xác " + overallAccuracy + "%. Bạn đã nắm phần lớn kiến thức, nhưng vẫn còn " + weakCount + " chủ đề cần củng cố để bứt phá điểm số.";
        if (overallAccuracy >= 50)
            return "Trung bình — độ chính xác " + overallAccuracy + "%. AI phát hiện " + weakCount + " lỗ hổng kiến thức cần lấp ngay, ưu tiên theo thứ tự đã liệt kê.";
        return "Bạn đang hổng kiến thức khá nhiều (độ chính xác chỉ " + overallAccuracy + "%). Đừng nản — hãy học lại từ gốc theo lộ trình AI đề xuất, ưu tiên từng chủ đề một.";
    }

    private String buildGapPrompt(int overallAccuracy, List<TopicStat> weak) {
        StringBuilder sb = new StringBuilder();
        sb.append("Học sinh đạt độ chính xác tổng thể ").append(overallAccuracy).append("% qua lịch sử làm bài thật.\n");
        if (weak.isEmpty()) {
            sb.append("Không có chủ đề nào bị hổng đáng kể.");
        } else {
            sb.append("Các chủ đề còn yếu (sắp xếp yếu nhất trước):\n");
            for (TopicStat t : weak) {
                sb.append("- ").append(t.topic()).append(" (môn ").append(t.subject())
                        .append("): sai ").append(t.wrong()).append("/").append(t.total())
                        .append(" câu, đúng ").append(accuracyPercent(t.total(), t.wrong())).append("%\n");
            }
        }
        sb.append("\nHãy viết nhận xét ngắn gọn dựa đúng trên dữ liệu này.");
        return sb.toString();
    }

    private String buildAdaptivePrompt(List<AdaptivePathViewResponse.SkillView> skills, List<TopicStat> weak) {
        StringBuilder sb = new StringBuilder();
        sb.append("Năng lực theo môn của học sinh:\n");
        for (AdaptivePathViewResponse.SkillView s : skills) {
            sb.append("- ").append(s.getSubject()).append(": ").append(s.getScore()).append("%\n");
        }
        if (!weak.isEmpty()) {
            sb.append("Chủ đề cần ưu tiên ôn tập nhất: ").append(weak.get(0).topic())
                    .append(" (môn ").append(weak.get(0).subject()).append(").\n");
        }
        sb.append("Hãy viết nhận xét ngắn gọn về lộ trình học tiếp theo dựa trên dữ liệu này.");
        return sb.toString();
    }

    private String buildUniversitySummary(String block, double predictedScore, boolean missingSubject,
                                           List<UniversitySuggestionView> suggestions) {
        String top = suggestions.isEmpty() ? "" : suggestions.get(0).getUniversity();
        String base = String.format(
                "Với điểm dự đoán khối %s hiện tại là %.1f/30, %s là lựa chọn có khả năng đỗ cao nhất trong danh sách gợi ý. "
                        + "Hãy tiếp tục luyện đề đều đặn để cải thiện các môn còn yếu, tăng cơ hội trúng tuyển nguyện vọng mong muốn.",
                block, predictedScore, top.isEmpty() ? "một trong các trường bên dưới" : top);
        if (missingSubject) {
            base += " Lưu ý: hệ thống hiện chưa có kho câu hỏi cho tất cả các môn của khối này nên một phần điểm được ước tính từ điểm trung bình chung của bạn.";
        }
        return base;
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
