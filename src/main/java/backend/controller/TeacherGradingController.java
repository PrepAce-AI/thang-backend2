package backend.controller;

import backend.service.TestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/grading")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TeacherGradingController {

    private final TestService testService;

    public TeacherGradingController(TestService testService) {
        this.testService = testService;
    }

    // API 1: Lấy danh sách các bài thi chờ chấm điểm công khai
    @GetMapping("/pending-sessions")
    public ResponseEntity<List<Map<String, Object>>> getPendingSessions() {
        return ResponseEntity.ok(testService.getPendingGradingSessions());
    }

    // API 2: Lấy chi tiết các câu trả lời tự luận để hiển thị lên form chấm bài
    @GetMapping("/session/{sessionId}/essay-answers")
    public ResponseEntity<List<Map<String, Object>>> getEssayAnswers(@PathVariable int sessionId) {
        return ResponseEntity.ok(testService.getEssayAnswersForTeacher(sessionId));
    }

    // API 3: Thực hiện cập nhật điểm số và nhận xét câu tự luận
    @PutMapping("/answer/{answerId}")
    public ResponseEntity<String> gradeAnswer(
            @PathVariable int answerId,
            @RequestBody Map<String, Object> body) {
        try {
            float score = Float.parseFloat(body.get("score").toString());
            String comment = (String) body.get("teacher_comment");

            testService.teacherGradeAnswer(answerId, score, comment);
            return ResponseEntity.ok("Chấm điểm thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý điểm: " + e.getMessage());
        }
    }
}