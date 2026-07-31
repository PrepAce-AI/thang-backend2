package backend.controller;

import backend.service.PracticeTestService; // 🔥 Đổi sang import bộ Service mới
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/grading")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class TeacherGradingController {

    // 🔥 Tiêu điểm: Tiêm bộ Service mới vào đây
    private final PracticeTestService practiceTestService;

    public TeacherGradingController(PracticeTestService practiceTestService) {
        this.practiceTestService = practiceTestService;
    }

    // API 1: Lấy danh sách các bài thi chờ chấm điểm (Đã map theo kiểu QuizAttempts)
    @GetMapping("/pending-sessions")
    public ResponseEntity<List<Map<String, Object>>> getPendingSessions() {
        return ResponseEntity.ok(practiceTestService.getPendingGradingAttempts());
    }

    // API 2: Lấy chi tiết các câu trả lời tự luận dựa theo attemptId (React gửi lên dưới tên sessionId)
    @GetMapping("/session/{sessionId}/essay-answers")
    public ResponseEntity<List<Map<String, Object>>> getEssayAnswers(@PathVariable int sessionId) {
        return ResponseEntity.ok(practiceTestService.getEssayAnswersForTeacher(sessionId));
    }

    // API 3: Giáo viên cập nhật điểm số và nhận xét câu tự luận
    @PutMapping("/answer/{answerId}")
    public ResponseEntity<String> gradeAnswer(
            @PathVariable int answerId,
            @RequestBody Map<String, Object> body) {
        try {
            double score = Double.parseDouble(body.get("score").toString());
            String comment = (String) body.get("teacher_comment");

            practiceTestService.teacherGradeAnswer(answerId, score, comment);
            return ResponseEntity.ok("Chấm điểm thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Lỗi xử lý điểm: " + e.getMessage());
        }
    }
}