package backend.controller;

import backend.dto.request.QuestionRequest;
import backend.dto.response.QuestionResponse;
import backend.service.AcademicQuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
public class AcademicQuestionController {

    @Autowired
    private AcademicQuestionService questionService;

    @Autowired
    private backend.repository.UserRepository userRepository;

    // API Đăng câu hỏi (Yêu cầu phải đăng nhập)
    @PostMapping
    public ResponseEntity<QuestionResponse> postQuestion(@RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.createQuestion(request));
    }

    // API Lấy danh sách câu hỏi của một bài học (Yêu cầu phải đăng nhập)
    @GetMapping("/lesson/{lessonId}")
    public ResponseEntity<List<QuestionResponse>> getQuestionsByLesson(@PathVariable Integer lessonId) {
        return ResponseEntity.ok(questionService.getQuestionsByLesson(lessonId));
    }

    // API Lấy toàn bộ câu hỏi của tất cả các khóa học do giáo viên quản lý
    @GetMapping("/teacher/all")
    public ResponseEntity<List<backend.dto.response.TeacherQuestionResponse>> getAllQuestionsForTeacher() {
        // Lấy email của User đang đăng nhập từ SecurityContext
        String currentUserEmail = (String) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        // Cần inject UserRepository hoặc lấy ID từ Jwt, để nhanh ta có thể truyền thẳng qua Service
        // Hoặc ta có thể đổi getAllQuestionsForTeacher nhận email thay vì teacherId, 
        // nhưng để giữ nguyên ta gọi userRepository ở Controller.
        // Tốt nhất là refactor một chút, nhưng ta có thể autowire ở đây:
        backend.entity.User user = userRepository.findByEmail(currentUserEmail)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        return ResponseEntity.ok(questionService.getAllQuestionsForTeacher(user.getId()));
    }

    // API Trả lời câu hỏi (Yêu cầu đăng nhập)
    @PostMapping("/{questionId}/answers")
    public ResponseEntity<backend.dto.response.AnswerResponse> postAnswer(
            @PathVariable Integer questionId,
            @RequestBody backend.dto.request.AnswerRequest request) {
        return ResponseEntity.ok(questionService.createAnswer(questionId, request));
    }

    // DEBUG API
    @GetMapping("/debug")
    public ResponseEntity<List<java.util.Map<String, Object>>> debugAllQuestions() {
        List<backend.entity.AcademicQuestion> questions = questionService.getAllRawQuestions();
        List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (backend.entity.AcademicQuestion q : questions) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("questionId", q.getId());
            map.put("content", q.getContent());
            map.put("userId", q.getUser() != null ? q.getUser().getId() : null);
            map.put("userName", q.getUser() != null ? q.getUser().getFullName() : null);
            map.put("lessonId", q.getLesson() != null ? q.getLesson().getId() : null);
            if (q.getLesson() != null && q.getLesson().getChapter() != null && q.getLesson().getChapter().getCourse() != null) {
                map.put("courseId", q.getLesson().getChapter().getCourse().getId());
                map.put("teacherId", q.getLesson().getChapter().getCourse().getTeacherId());
            }
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}