package backend.controller;

import backend.dto.request.ChatRequest;
import backend.dto.request.UniversityAdvisingRequest;
import backend.dto.response.AdaptivePathViewResponse;
import backend.dto.response.ChatResponse;
import backend.dto.response.GapDiagnosisResponse;
import backend.dto.response.ScoreForecastResponse;
import backend.dto.response.UniversityAdvisingResponse;
import backend.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * UC-26: Consult AI Chatbot
 * UC-27: Adaptive Path Generation
 * UC-28: AI Gap Diagnosis
 * UC-29: AI Score Forecasting
 * UC-30: AI University Advising
 *
 * Base URL: /api/ai
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    // ─── UC-26: Chat ─────────────────────────────────────────────────────────────

    /** Gửi câu hỏi tới AI Chatbot (Gemini) */
    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestHeader("X-Student-Id") Integer studentId,
            @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(aiService.chat(studentId, request));
    }

    /** Lịch sử chat của học sinh (phân trang) */
    @GetMapping("/chat/history")
    public ResponseEntity<Page<ChatResponse>> getChatHistory(
            @RequestHeader("X-Student-Id") Integer studentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(aiService.getChatHistory(studentId, page, size));
    }

    // ─── UC-28: Gap Diagnosis ────────────────────────────────────────────────────

    /**
     * AI phân tích lỗ hổng kiến thức dựa trên lịch sử làm bài.
     * Trả về: điểm trung bình, biểu đồ cognitive, danh sách gap.
     */
    @GetMapping("/gap-diagnosis")
    public ResponseEntity<GapDiagnosisResponse> diagnoseGaps(
            @RequestHeader("X-Student-Id") Integer studentId) {
        return ResponseEntity.ok(aiService.diagnoseGaps(studentId));
    }

    // ─── UC-27: Adaptive Path ────────────────────────────────────────────────────

    /**
     * Sinh lộ trình học thích ứng dựa trên năng lực thực tế.
     * Trả về: biểu đồ năng lực + danh sách bài học ưu tiên.
     */
    @GetMapping("/adaptive-path")
    public ResponseEntity<AdaptivePathViewResponse> generatePath(
            @RequestHeader("X-Student-Id") Integer studentId) {
        return ResponseEntity.ok(aiService.generateAdaptivePath(studentId));
    }

    // ─── UC-29: Score Forecasting ────────────────────────────────────────────────

    /** Dự đoán điểm thi THPT QG dựa trên phong độ học sinh */
    @GetMapping("/score-forecast")
    public ResponseEntity<ScoreForecastResponse> forecastScore(
            @RequestHeader("X-Student-Id") Integer studentId) {
        return ResponseEntity.ok(aiService.forecastScore(studentId));
    }

    // ─── UC-30: University Advising ──────────────────────────────────────────────

    /**
     * Tư vấn ngành và trường đại học phù hợp theo khối thi (A00/A01/B00/C00/D01).
     */
    @GetMapping("/university-advising")
    public ResponseEntity<UniversityAdvisingResponse> universityAdvising(
            @RequestHeader("X-Student-Id") Integer studentId,
            @RequestParam(defaultValue = "A00") String block){

        return ResponseEntity.ok(
                aiService.getUniversityAdvising(studentId, block)
        );
    }

    @PostMapping("/university-advising")
    public UniversityAdvisingResponse getUniversityAdvising(
            @RequestBody UniversityAdvisingRequest request
    ) {
        return aiService.getUniversityAdvising(
                request.getStudentId(),
                request.getBlock()
        );
    }
}
