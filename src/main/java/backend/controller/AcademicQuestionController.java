package backend.controller;

import backend.dto.request.AnswerReportRequest;
import backend.dto.request.AnswerRequest;
import backend.dto.request.QuestionRequest;
import backend.dto.response.AnswerResponse;
import backend.dto.response.QuestionResponse;
import backend.dto.response.TeacherQuestionResponse;
import backend.entity.AcademicAnswer;
import backend.repository.AcademicAnswerRepository;
import backend.service.AcademicQuestionService;
import backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@CrossOrigin(origins = "*") // Đảm bảo Frontend gọi được
public class AcademicQuestionController {

    @Autowired
    private AcademicQuestionService questionService;
    private EmailService emailService;
    private final AcademicAnswerRepository answerRepository;

    public AcademicQuestionController(AcademicAnswerRepository answerRepository) {
        this.answerRepository = answerRepository;
    }

    // 1. Đăng câu hỏi mới
    @PostMapping("")
    public ResponseEntity<QuestionResponse> createQuestion(@RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.createQuestion(request));
    }

    // 2. Lấy danh sách câu hỏi theo bài học
    // 🔥 ĐÃ FIX: Sử dụng String cho lessonId để tránh lỗi 404 nếu Frontend gửi sai định dạng (như 14:1)
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByLesson(@PathVariable String lessonId) {
        // Ép kiểu an toàn: nếu có dấu ":" (ví dụ 14:1), lấy phần trước dấu :
        Integer id = Integer.parseInt(lessonId.split(":")[0]);
        return ResponseEntity.ok(questionService.getQuestionsByLesson(id));
    }

    // 3. Đăng câu trả lời
    @PostMapping("/{questionId}/answers")
    public ResponseEntity<AnswerResponse> createAnswer(@PathVariable Integer questionId, @RequestBody AnswerRequest request) {
        return ResponseEntity.ok(questionService.createAnswer(questionId, request));
    }

    // 4. Admin xóa câu hỏi
    @DeleteMapping("/{questionId}")
    public ResponseEntity<String> deleteQuestion(@PathVariable Integer questionId) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.ok("Đã xóa câu hỏi thành công");
    }

    // 5. Admin xóa câu trả lời (lưu ý: bạn cần đổi route nếu cần thiết, hoặc map chung vào /answers)
    @DeleteMapping("/answers/{answerId}")
    public ResponseEntity<String> deleteAnswer(@PathVariable Integer answerId) {
        questionService.deleteAnswer(answerId);
        return ResponseEntity.ok("Đã xóa câu trả lời thành công");
    }

    // 6. Lấy câu hỏi cho giáo viên
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<TeacherQuestionResponse>> getAllQuestionsForTeacher(@PathVariable Integer teacherId) {
        return ResponseEntity.ok(questionService.getAllQuestionsForTeacher(teacherId));
    }

    //7. Report answer
    @PostMapping("/answers/{answerId}/report")
    public ResponseEntity<?> reportAnswer(
            @PathVariable Integer answerId,
            @RequestBody Map<String, String> request
    ) {
        questionService.reportAnswer(
                answerId,
                request.get("reason")
        );

        return ResponseEntity.ok("Đã gửi báo cáo");
    }
}