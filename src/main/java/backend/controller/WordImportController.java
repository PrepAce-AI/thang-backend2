package backend.controller;

import backend.service.WordImportService;
import backend.service.WordImportService.ImportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * =====================================================================
 * WordImportController – REST API cho tính năng Tạo Đề từ File Word
 * =====================================================================
 * Base URL: /api/word-import
 *
 * Luồng:
 *   1. POST /api/word-import/preview  → Parse file, trả preview (KHÔNG lưu DB)
 *   2. POST /api/word-import/confirm  → Parse file lần 2, lưu vào DB
 * =====================================================================
 */
@RestController
@RequestMapping("/api/word-import")
@RequiredArgsConstructor
@CrossOrigin(
        origins = "http://localhost:5173",
        allowedHeaders = "*",
        methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
public class WordImportController {

    private final WordImportService wordImportService;

    /**
     * BƯỚC 1 – Preview: Phân tích file Word, trả về thống kê + 5 câu đầu.
     * KHÔNG lưu vào database.
     *
     * Request: multipart/form-data
     *   - file: file .docx
     *
     * Response: { totalQuestions, multipleChoiceCount, trueFalseCount, shortAnswerCount, previewQuestions }
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> previewWord(@RequestParam("file") MultipartFile file) {
        try {
            // Kiểm tra định dạng file
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Chỉ hỗ trợ file .docx (Word 2007+). " +
                               "File .doc cũ cần convert sang .docx trước.")
                );
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "File rỗng. Vui lòng chọn lại file.")
                );
            }

            ImportResult result = wordImportService.previewFromWord(file);
            return ResponseEntity.ok(buildResponse(result, false));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Lỗi khi đọc file Word: " + e.getMessage())
            );
        }
    }

    /**
     * BƯỚC 2 – Confirm & Lưu: Parse lại và lưu vào database.
     *
     * Request: multipart/form-data
     *   - file        : file .docx (giống lúc preview)
     *   - quizTitle   : Tên đề thi
     *   - subject     : Môn học (math / physics / chemistry / english / literature...)
     *   - duration    : Thời gian làm bài (phút), default 90
     *   - courseId    : ID khóa học liên kết (optional, có thể bỏ trống)
     *
     * Response: { quizId, quizTitle, totalQuestions, breakdown }
     */
    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> confirmImport(
            @RequestParam("file")                         MultipartFile file,
            @RequestParam("quizTitle")                    String quizTitle,
            @RequestParam(value = "subject",    defaultValue = "general") String subject,
            @RequestParam(value = "duration",   defaultValue = "90")      int duration,
            @RequestParam(value = "courseId",   required = false)         Integer courseId
    ) {
        try {
            // Kiểm tra định dạng file
            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Chỉ hỗ trợ file .docx (Word 2007+).")
                );
            }

            if (quizTitle == null || quizTitle.isBlank()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Vui lòng nhập tên đề thi.")
                );
            }

            if (duration <= 0 || duration > 300) {
                duration = 90; // Default 90 phút nếu nhập sai
            }

            // Parse và lưu DB
            ImportResult result = wordImportService.importFromWord(
                    file, quizTitle.trim(), subject, duration, courseId
            );

            return ResponseEntity.ok(buildResponse(result, true));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(
                    Map.of("error", "Lỗi khi nhập đề vào hệ thống: " + e.getMessage())
            );
        }
    }

    // ─── Helper ───────────────────────────────────────────────────────────────────

    /**
     * Xây dựng response JSON thân thiện cho frontend.
     */
    private Map<String, Object> buildResponse(ImportResult result, boolean saved) {
        return Map.of(
                "quizId",             result.quizId,
                "quizTitle",          result.quizTitle != null ? result.quizTitle : "",
                "saved",              saved,
                "totalQuestions",     result.totalQuestions,
                "multipleChoiceCount",result.multipleChoiceCount,
                "trueFalseCount",     result.trueFalseCount,
                "shortAnswerCount",   result.shortAnswerCount,
                "previewQuestions",   formatPreview(result.previewQuestions),
                "warnings",           result.warnings
        );
    }

    /**
     * Chuyển ParsedQuestion list thành danh sách Map để serialize JSON.
     */
    private java.util.List<Map<String, Object>> formatPreview(
            java.util.List<WordImportService.ParsedQuestion> questions
    ) {
        if (questions == null) return java.util.Collections.emptyList();
        java.util.List<Map<String, Object>> out = new java.util.ArrayList<>();
        for (WordImportService.ParsedQuestion q : questions) {
            java.util.List<Map<String, Object>> opts = new java.util.ArrayList<>();
            for (WordImportService.ParsedQuestion.OptionItem oi : q.options) {
                opts.add(Map.of(
                        "label",     oi.label,
                        "content",   oi.content,
                        "isCorrect", oi.isCorrect
                ));
            }
            out.add(Map.of(
                    "number",        q.number,
                    "type",          q.type != null ? q.type : "SHORT_ANSWER",
                    "content",       q.content != null ? q.content : "",
                    "correctAnswer", q.correctAnswer != null ? q.correctAnswer : "",
                    "options",       opts
            ));
        }
        return out;
    }
}
